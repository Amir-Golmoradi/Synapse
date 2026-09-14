package dev.amir.synapse.messaging.domain.port.in.send_video_message;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import dev.amir.synapse.messaging.domain.exception.MessageValidationException;
import java.io.ByteArrayInputStream;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class SendVideoMessageCommandTest {

  @Test
  void rejectsMissingIdentifiersContentTypeAndContent() {
    var id = UUID.randomUUID();

    assertThatThrownBy(
            () ->
                new SendVideoMessageCommand(
                    null, id, id, "video/webm", 1, () -> new ByteArrayInputStream(new byte[1])))
        .isInstanceOf(MessageValidationException.class);
    assertThatThrownBy(
            () ->
                new SendVideoMessageCommand(
                    id, id, id, " ", 1, () -> new ByteArrayInputStream(new byte[1])))
        .isInstanceOf(MessageValidationException.class);
    assertThatThrownBy(() -> new SendVideoMessageCommand(id, id, id, "video/webm", 0, null))
        .isInstanceOf(MessageValidationException.class);
  }
}
