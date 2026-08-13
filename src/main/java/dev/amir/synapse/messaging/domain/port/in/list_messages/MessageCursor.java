package dev.amir.synapse.messaging.domain.port.in.list_messages;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record MessageCursor(Instant createdAt, UUID messageId) {
  public MessageCursor {
    Objects.requireNonNull(createdAt, "Cursor timestamp cannot be null");
    Objects.requireNonNull(messageId, "Cursor message ID cannot be null");
  }
}
