package dev.amir.synapse.messaging.application.command.send_video_message;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import dev.amir.synapse.messaging.application.model.VideoMessageSettings;
import dev.amir.synapse.messaging.application.service.VideoMessageTransactionService;
import dev.amir.synapse.messaging.domain.enums.MessageType;
import dev.amir.synapse.messaging.domain.exception.MessageIdempotencyConflictException;
import dev.amir.synapse.messaging.domain.exception.MessageRoomAccessDeniedException;
import dev.amir.synapse.messaging.domain.exception.VideoMessageException;
import dev.amir.synapse.messaging.domain.port.in.message.MessageView;
import dev.amir.synapse.messaging.domain.port.in.send_video_message.SendVideoMessageCommand;
import dev.amir.synapse.messaging.domain.port.in.send_video_message.SendVideoMessageResult;
import dev.amir.synapse.messaging.domain.port.out.MessageAuthorizationPort;
import dev.amir.synapse.messaging.domain.port.out.MessageMediaStoragePort;
import dev.amir.synapse.messaging.domain.port.out.MessageMediaStoragePort.StagedMedia;
import dev.amir.synapse.messaging.domain.port.out.MessagePersistenceResult;
import dev.amir.synapse.messaging.domain.port.out.VideoMediaInspectionPort;
import dev.amir.synapse.messaging.domain.value_object.VideoMetadata;
import dev.amir.synapse.messaging.domain.value_object.VideoMetadata.AudioCodec;
import dev.amir.synapse.messaging.domain.value_object.VideoMetadata.VideoCodec;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.io.ByteArrayInputStream;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class SendVideoMessageHandlerTest {
  private static final byte[] DIGEST = new byte[32];

  private MessageAuthorizationPort authorization;
  private MessageMediaStoragePort storage;
  private VideoMediaInspectionPort inspection;
  private VideoMessageTransactionService transaction;
  private SendVideoMessageHandler handler;

  @BeforeEach
  void setUp() {
    authorization = mock(MessageAuthorizationPort.class);
    storage = mock(MessageMediaStoragePort.class);
    inspection = mock(VideoMediaInspectionPort.class);
    transaction = mock(VideoMessageTransactionService.class);
    handler =
        new SendVideoMessageHandler(
            authorization, storage, inspection, transaction, settings(), new SimpleMeterRegistry());
  }

  @Test
  void stagesInspectsPromotesAndCreatesTheMessage() {
    var command = command(100);
    var staged = new StagedMedia(Path.of("staged.part"), 100, DIGEST);
    var metadata = metadata(1_000, 640, 480);
    var persisted = new MessagePersistenceResult(message(command, metadata), true);
    when(authorization.canSend(command.roomId(), command.senderId())).thenReturn(true);
    when(storage.stage(any(), eq(1_000L))).thenReturn(staged);
    when(inspection.inspect(staged.path(), "video/webm", 100)).thenReturn(metadata);
    when(storage.promote(staged, "video/webm")).thenReturn("video/aa/id.webm");
    when(transaction.create(
            command.roomId(),
            command.senderId(),
            command.clientMessageId(),
            "video/aa/id.webm",
            DIGEST,
            metadata))
        .thenReturn(persisted);

    var result = handler.handle(command);

    assertThat(result).isEqualTo(new SendVideoMessageResult(persisted.message(), true));
    verify(storage).discard(staged);
    verify(storage, never()).delete("video/aa/id.webm");
  }

  @Test
  void rejectsOversizedAndUnauthorizedRequestsBeforeReadingContent() {
    assertThatThrownBy(() -> handler.handle(command(1_001)))
        .isInstanceOf(VideoMessageException.class)
        .extracting(exception -> ((VideoMessageException) exception).getErrorCode())
        .isEqualTo("VIDEO_FILE_TOO_LARGE");
    verifyNoInteractions(authorization, storage, inspection, transaction);

    var unauthorized = command(100);
    when(authorization.canSend(unauthorized.roomId(), unauthorized.senderId())).thenReturn(false);
    assertThatThrownBy(() -> handler.handle(unauthorized))
        .isInstanceOf(MessageRoomAccessDeniedException.class);
    verifyNoInteractions(storage, inspection, transaction);
  }

  @Test
  void discardsStagingWhenInspectionRejectsTheVideo() {
    var command = command(100);
    var staged = new StagedMedia(Path.of("staged.part"), 100, DIGEST);
    when(authorization.canSend(command.roomId(), command.senderId())).thenReturn(true);
    when(storage.stage(any(), eq(1_000L))).thenReturn(staged);
    when(inspection.inspect(staged.path(), "video/webm", 100))
        .thenThrow(VideoMessageException.unsupported("bad codec"));

    assertThatThrownBy(() -> handler.handle(command)).isInstanceOf(VideoMessageException.class);

    verify(storage).discard(staged);
    verify(storage, never()).promote(any(), any());
  }

  @Test
  void compensatesPromotedObjectWhenDatabaseCreationFails() {
    var command = command(100);
    var staged = new StagedMedia(Path.of("staged.part"), 100, DIGEST);
    var metadata = metadata(1_000, 640, 480);
    when(authorization.canSend(command.roomId(), command.senderId())).thenReturn(true);
    when(storage.stage(any(), eq(1_000L))).thenReturn(staged);
    when(inspection.inspect(staged.path(), "video/webm", 100)).thenReturn(metadata);
    when(storage.promote(staged, "video/webm")).thenReturn("video/aa/id.webm");
    when(transaction.create(any(), any(), any(), any(), any(), any()))
        .thenThrow(new MessageIdempotencyConflictException());

    assertThatThrownBy(() -> handler.handle(command))
        .isInstanceOf(MessageIdempotencyConflictException.class);

    verify(storage).delete("video/aa/id.webm");
    verify(storage).discard(staged);
  }

  @Test
  void removesRedundantObjectForAnExactRetry() {
    var command = command(100);
    var staged = new StagedMedia(Path.of("staged.part"), 100, DIGEST);
    var metadata = metadata(1_000, 640, 480);
    var persisted = new MessagePersistenceResult(message(command, metadata), false);
    when(authorization.canSend(command.roomId(), command.senderId())).thenReturn(true);
    when(storage.stage(any(), eq(1_000L))).thenReturn(staged);
    when(inspection.inspect(staged.path(), "video/webm", 100)).thenReturn(metadata);
    when(storage.promote(staged, "video/webm")).thenReturn("video/aa/retry.webm");
    when(transaction.create(any(), any(), any(), any(), any(), any())).thenReturn(persisted);

    var result = handler.handle(command);

    assertThat(result.created()).isFalse();
    verify(storage).delete("video/aa/retry.webm");
  }

  @Test
  void enforcesDurationAndDimensionLimitsBeforePromotion() {
    for (var metadata :
        java.util.List.of(metadata(60_001, 640, 480), metadata(1_000, 1_921, 480))) {
      var command = command(100);
      var staged = new StagedMedia(Path.of(UUID.randomUUID() + ".part"), 100, DIGEST);
      when(authorization.canSend(command.roomId(), command.senderId())).thenReturn(true);
      when(storage.stage(any(), eq(1_000L))).thenReturn(staged);
      when(inspection.inspect(staged.path(), "video/webm", 100)).thenReturn(metadata);

      assertThatThrownBy(() -> handler.handle(command)).isInstanceOf(VideoMessageException.class);
      verify(storage).discard(staged);
    }
    verify(storage, never()).promote(any(), any());
  }

  private static SendVideoMessageCommand command(long size) {
    return new SendVideoMessageCommand(
        UUID.randomUUID(),
        UUID.randomUUID(),
        UUID.randomUUID(),
        "video/webm",
        size,
        () -> new ByteArrayInputStream(new byte[(int) size]));
  }

  private static VideoMetadata metadata(long duration, int width, int height) {
    return new VideoMetadata(
        "video/webm", 100, duration, width, height, VideoCodec.VP9, AudioCodec.OPUS);
  }

  private static MessageView message(SendVideoMessageCommand command, VideoMetadata metadata) {
    return new MessageView(
        UUID.randomUUID(),
        command.roomId(),
        command.senderId(),
        command.clientMessageId(),
        MessageType.VIDEO,
        null,
        metadata,
        Instant.parse("2026-09-13T00:00:00Z"));
  }

  private static VideoMessageSettings settings() {
    return new VideoMessageSettings(
        Path.of("media"),
        1_000,
        Duration.ofSeconds(60),
        1_920,
        2_073_600,
        Duration.ofSeconds(5),
        Duration.ofHours(1),
        Duration.ofHours(24),
        100);
  }
}
