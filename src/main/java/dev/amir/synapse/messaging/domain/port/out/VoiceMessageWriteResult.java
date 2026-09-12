package dev.amir.synapse.messaging.domain.port.out;

import dev.amir.synapse.messaging.domain.port.in.message.MessageView;
import java.util.Objects;

public record VoiceMessageWriteResult(MessageView message, boolean created) {
  public VoiceMessageWriteResult {
    Objects.requireNonNull(message, "Message cannot be null");
  }
}
