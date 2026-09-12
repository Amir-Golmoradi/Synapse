package dev.amir.synapse.messaging.application.model;

import dev.amir.synapse.messaging.domain.port.in.message.MessageView;
import java.util.Objects;

public record MessageCommittedNotification(MessageView message) {
  public MessageCommittedNotification {
    Objects.requireNonNull(message, "Message cannot be null");
  }
}
