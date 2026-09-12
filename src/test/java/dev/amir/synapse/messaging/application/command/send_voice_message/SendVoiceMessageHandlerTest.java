package dev.amir.synapse.messaging.application.command.send_voice_message;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import dev.amir.synapse.messaging.application.model.StoredVoiceMedia;
import dev.amir.synapse.messaging.application.port.out.VoiceMediaStoragePort;
import dev.amir.synapse.messaging.application.service.VoiceMessageTransactionService;
import dev.amir.synapse.messaging.domain.enums.MessageType;
import dev.amir.synapse.messaging.domain.exception.MessageRoomAccessDeniedException;
import dev.amir.synapse.messaging.domain.exception.VoiceMediaStorageException;
import dev.amir.synapse.messaging.domain.port.in.message.MessageView;
import dev.amir.synapse.messaging.domain.port.in.send_voice_message.SendVoiceMessageCommand;
import dev.amir.synapse.messaging.domain.port.out.MessageSendAuthorizationPort;
import dev.amir.synapse.messaging.domain.port.out.VoiceMessageWriteResult;
import dev.amir.synapse.messaging.domain.value_object.VoiceMessageMetadata;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.io.ByteArrayInputStream;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class SendVoiceMessageHandlerTest {
  private final MessageSendAuthorizationPort authorizationPort =
      mock(MessageSendAuthorizationPort.class);
  private final VoiceMediaStoragePort storagePort = mock(VoiceMediaStoragePort.class);
  private final VoiceMessageTransactionService transactionService =
      mock(VoiceMessageTransactionService.class);
  private final SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
  private final SendVoiceMessageHandler handler =
      new SendVoiceMessageHandler(
          authorizationPort, storagePort, transactionService, meterRegistry);

  @Test
  void storesAndCommitsAuthorizedVoiceMessage() {
    var command = command();
    var stored = stored();
    var canonical = view(command);
    when(authorizationPort.canSend(command.roomId(), command.senderId())).thenReturn(true);
    when(storagePort.store(any(), anyString(), anyLong(), any())).thenReturn(stored);
    when(transactionService.create(any(), any(), any()))
        .thenReturn(new VoiceMessageWriteResult(canonical, true));

    var result = handler.handle(command);

    assertThat(result.created()).isTrue();
    assertThat(result.message()).isEqualTo(canonical);
    verify(storagePort, never()).delete(any());
  }

  @Test
  void deletesLosingCandidateForExactReplay() {
    var command = command();
    var stored = stored();
    var canonical = view(command);
    when(authorizationPort.canSend(command.roomId(), command.senderId())).thenReturn(true);
    when(storagePort.store(any(), anyString(), anyLong(), any())).thenReturn(stored);
    when(transactionService.create(any(), any(), any()))
        .thenReturn(new VoiceMessageWriteResult(canonical, false));

    var result = handler.handle(command);

    assertThat(result.created()).isFalse();
    verify(storagePort).delete(stored.storageKey());
  }

  @Test
  void rejectsBeforeStorageWhenSenderCannotSend() {
    var command = command();
    when(authorizationPort.canSend(command.roomId(), command.senderId())).thenReturn(false);

    assertThatThrownBy(() -> handler.handle(command))
        .isInstanceOf(MessageRoomAccessDeniedException.class);

    verify(storagePort, never()).store(any(), anyString(), anyLong(), any());
  }

  @Test
  void deletesCandidateWhenDatabaseTransactionFails() {
    var command = command();
    var stored = stored();
    when(authorizationPort.canSend(command.roomId(), command.senderId())).thenReturn(true);
    when(storagePort.store(any(), anyString(), anyLong(), any())).thenReturn(stored);
    when(transactionService.create(any(), any(), any()))
        .thenThrow(new IllegalStateException("database unavailable"));

    assertThatThrownBy(() -> handler.handle(command)).isInstanceOf(IllegalStateException.class);

    verify(storagePort).delete(stored.storageKey());
  }

  @Test
  void recordsStorageFailureWithoutAttemptingCandidateDeletion() {
    var command = command();
    when(authorizationPort.canSend(command.roomId(), command.senderId())).thenReturn(true);
    when(storagePort.store(any(), anyString(), anyLong(), any()))
        .thenThrow(
            new VoiceMediaStorageException("storage unavailable", new java.io.IOException()));

    assertThatThrownBy(() -> handler.handle(command))
        .isInstanceOf(VoiceMediaStorageException.class);

    verify(storagePort, never()).delete(any());
    assertThat(
            meterRegistry
                .find("synapse.voice.messages.upload")
                .tags("outcome", "failed", "mime", "audio/ogg")
                .timer())
        .isNotNull()
        .extracting(io.micrometer.core.instrument.Timer::count)
        .isEqualTo(1L);
  }

  private static SendVoiceMessageCommand command() {
    var bytes = "OggS--OpusHead--voice".getBytes(java.nio.charset.StandardCharsets.US_ASCII);
    return new SendVoiceMessageCommand(
        UUID.randomUUID(),
        UUID.randomUUID(),
        UUID.randomUUID(),
        12_000,
        "audio/ogg",
        bytes.length,
        () -> new ByteArrayInputStream(bytes));
  }

  private static StoredVoiceMedia stored() {
    return new StoredVoiceMedia("voice/ab/" + UUID.randomUUID(), "audio/ogg", 20, new byte[32]);
  }

  private static MessageView view(SendVoiceMessageCommand command) {
    return new MessageView(
        UUID.randomUUID(),
        command.roomId(),
        command.senderId(),
        command.clientMessageId(),
        MessageType.VOICE,
        null,
        new VoiceMessageMetadata(command.durationMs(), "audio/ogg", 20),
        Instant.parse("2026-09-12T12:00:00Z"));
  }
}
