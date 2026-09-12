package dev.amir.synapse.messaging.application.query.get_voice_media;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import dev.amir.synapse.messaging.application.port.out.VoiceMediaStoragePort;
import dev.amir.synapse.messaging.domain.exception.InvalidMediaRangeException;
import dev.amir.synapse.messaging.domain.exception.MessageMediaNotFoundException;
import dev.amir.synapse.messaging.domain.port.in.get_voice_media.GetVoiceMediaQuery;
import dev.amir.synapse.messaging.domain.port.in.get_voice_media.MediaRangeRequest;
import dev.amir.synapse.messaging.domain.port.out.VoiceMediaRecord;
import dev.amir.synapse.messaging.domain.port.out.VoiceMessageMediaPort;
import java.io.ByteArrayInputStream;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class GetVoiceMediaHandlerTest {
  private final VoiceMessageMediaPort mediaPort = mock(VoiceMessageMediaPort.class);
  private final VoiceMediaStoragePort storagePort = mock(VoiceMediaStoragePort.class);
  private final GetVoiceMediaHandler handler = new GetVoiceMediaHandler(mediaPort, storagePort);

  @Test
  void resolvesClosedRangeAndOpensAtItsOffset() {
    var requesterId = UUID.randomUUID();
    var roomId = UUID.randomUUID();
    var messageId = UUID.randomUUID();
    var media = media(roomId, messageId);
    when(mediaPort.findAuthorized(roomId, messageId, requesterId)).thenReturn(Optional.of(media));
    when(storagePort.open(media.storageKey(), 10))
        .thenReturn(new ByteArrayInputStream(new byte[90]));

    var download =
        handler.handle(
            new GetVoiceMediaQuery(
                requesterId, roomId, messageId, new MediaRangeRequest(10L, 19L, null)));

    assertThat(download.partial()).isTrue();
    assertThat(download.offset()).isEqualTo(10);
    assertThat(download.contentLength()).isEqualTo(10);
    verify(storagePort).open(media.storageKey(), 10);
  }

  @Test
  void resolvesSuffixRangeAgainstActualSize() {
    var requesterId = UUID.randomUUID();
    var roomId = UUID.randomUUID();
    var messageId = UUID.randomUUID();
    var media = media(roomId, messageId);
    when(mediaPort.findAuthorized(roomId, messageId, requesterId)).thenReturn(Optional.of(media));
    when(storagePort.open(media.storageKey(), 75))
        .thenReturn(new ByteArrayInputStream(new byte[25]));

    var download =
        handler.handle(
            new GetVoiceMediaQuery(
                requesterId, roomId, messageId, new MediaRangeRequest(null, null, 25L)));

    assertThat(download.offset()).isEqualTo(75);
    assertThat(download.contentLength()).isEqualTo(25);
  }

  @Test
  void hidesMissingOrUnauthorizedMediaAndRejectsUnsatisfiableRange() {
    var requesterId = UUID.randomUUID();
    var roomId = UUID.randomUUID();
    var messageId = UUID.randomUUID();
    when(mediaPort.findAuthorized(roomId, messageId, requesterId)).thenReturn(Optional.empty());
    assertThatThrownBy(
            () -> handler.handle(new GetVoiceMediaQuery(requesterId, roomId, messageId, null)))
        .isInstanceOf(MessageMediaNotFoundException.class);

    var media = media(roomId, messageId);
    when(mediaPort.findAuthorized(roomId, messageId, requesterId)).thenReturn(Optional.of(media));
    assertThatThrownBy(
            () ->
                handler.handle(
                    new GetVoiceMediaQuery(
                        requesterId, roomId, messageId, new MediaRangeRequest(100L, null, null))))
        .isInstanceOf(InvalidMediaRangeException.class);
  }

  private static VoiceMediaRecord media(UUID roomId, UUID messageId) {
    return new VoiceMediaRecord(
        messageId,
        roomId,
        "voice/ab/" + messageId,
        "audio/ogg",
        100,
        1_000,
        Instant.parse("2026-09-12T12:00:00Z"));
  }
}
