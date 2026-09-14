package dev.amir.synapse.messaging.application.query.get_message_media;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import dev.amir.synapse.messaging.domain.exception.VideoMessageException;
import dev.amir.synapse.messaging.domain.port.out.MessageMediaReadPort;
import dev.amir.synapse.messaging.domain.port.out.MessageMediaReadPort.MessageMediaDescriptor;
import dev.amir.synapse.messaging.domain.port.out.MessageMediaStoragePort;
import java.nio.file.Path;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class GetMessageMediaHandlerTest {

  @Test
  void authorizesThroughPersistenceBeforeResolvingPrivateStorage() {
    var media = mock(MessageMediaReadPort.class);
    var storage = mock(MessageMediaStoragePort.class);
    var handler = new GetMessageMediaHandler(media, storage);
    var messageId = UUID.randomUUID();
    var requesterId = UUID.randomUUID();
    var digest = new byte[32];
    var descriptor =
        new MessageMediaDescriptor(messageId, "video/aa/id.webm", "video/webm", 20, digest);
    when(media.findAuthorized(messageId, requesterId)).thenReturn(Optional.of(descriptor));
    when(storage.resolveForRead("video/aa/id.webm")).thenReturn(Path.of("media.webm"));

    var result = handler.handle(messageId, requesterId);

    assertThat(result.path()).isEqualTo(Path.of("media.webm"));
    assertThat(result.contentType()).isEqualTo("video/webm");
    assertThat(result.sha256()).containsExactly(digest);
  }

  @Test
  void missingAndUnauthorizedMediaHaveTheSameNotFoundResult() {
    var media = mock(MessageMediaReadPort.class);
    var storage = mock(MessageMediaStoragePort.class);
    var handler = new GetMessageMediaHandler(media, storage);
    var messageId = UUID.randomUUID();
    var requesterId = UUID.randomUUID();
    when(media.findAuthorized(messageId, requesterId)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> handler.handle(messageId, requesterId))
        .isInstanceOf(VideoMessageException.class)
        .extracting(exception -> ((VideoMessageException) exception).getHttpStatus())
        .isEqualTo(404);
    verifyNoInteractions(storage);
  }
}
