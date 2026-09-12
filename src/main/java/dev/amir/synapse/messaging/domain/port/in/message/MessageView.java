package dev.amir.synapse.messaging.domain.port.in.message;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record MessageView(
    UUID messageId,
    UUID roomId,
    UUID senderId,
    UUID clientMessageId,
    String text,
    Instant createdAt) {

  public MessageView {
    Objects.requireNonNull(messageId, "Message ID cannot be null");
    Objects.requireNonNull(roomId, "Room ID cannot be null");
    Objects.requireNonNull(senderId, "Sender ID cannot be null");
    Objects.requireNonNull(clientMessageId, "Client message ID cannot be null");
    Objects.requireNonNull(text, "Message text cannot be null");
    Objects.requireNonNull(createdAt, "Message creation timestamp cannot be null");
  }
}
