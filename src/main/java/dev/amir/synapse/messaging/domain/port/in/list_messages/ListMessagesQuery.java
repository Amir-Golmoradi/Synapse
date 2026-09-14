package dev.amir.synapse.messaging.domain.port.in.list_messages;

import java.util.Objects;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

public record ListMessagesQuery(
    UUID requesterId, UUID roomId, int limit, @Nullable MessageCursor cursor) {
  public static final int MIN_LIMIT = 1;
  public static final int MAX_LIMIT = 100;

  public ListMessagesQuery {
    Objects.requireNonNull(requesterId, "Requester ID cannot be null");
    Objects.requireNonNull(roomId, "Room ID cannot be null");
    if (limit < MIN_LIMIT || limit > MAX_LIMIT) {
      throw new IllegalArgumentException(
          "Message history limit must be between " + MIN_LIMIT + " and " + MAX_LIMIT + ".");
    }
  }
}
