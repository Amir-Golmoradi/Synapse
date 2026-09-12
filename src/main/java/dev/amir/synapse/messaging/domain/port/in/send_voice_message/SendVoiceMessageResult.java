package dev.amir.synapse.messaging.domain.port.in.send_voice_message;

import dev.amir.synapse.messaging.domain.port.in.message.MessageView;
import java.util.Objects;

public record SendVoiceMessageResult(MessageView message, boolean created) {
  public SendVoiceMessageResult {
    Objects.requireNonNull(message, "Message cannot be null");
  }
}
