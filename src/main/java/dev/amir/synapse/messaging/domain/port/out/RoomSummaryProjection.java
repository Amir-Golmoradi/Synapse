package dev.amir.synapse.messaging.domain.port.out;

import dev.amir.synapse.messaging.domain.enums.RoomStatus;
import dev.amir.synapse.messaging.domain.enums.RoomType;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

public record RoomSummaryProjection(
    UUID roomId,
    RoomType type,
    RoomStatus status,
    @Nullable String name,
    @Nullable String avatarUrl,
    int memberCount,
    Instant createdAt,
    Instant lastMessagesAt) {
  private static final int MIN_MEMBER_COUNT = 1;

  public RoomSummaryProjection {
    Objects.requireNonNull(roomId, "Room ID cannot be null");
    Objects.requireNonNull(type, "Room type cannot be null");
    Objects.requireNonNull(status, "Room status cannot be null");
    Objects.requireNonNull(createdAt, "Created timestamp cannot be null");
    Objects.requireNonNull(lastMessagesAt, "Last message timestamp cannot be null");
    if (memberCount < MIN_MEMBER_COUNT) {
      throw new IllegalArgumentException("Member count must be positive.");
    }
  }
}
