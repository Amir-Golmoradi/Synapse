package dev.amir.synapse.messaging.domain.port.out;

import dev.amir.synapse.messaging.domain.enums.RoomStatus;
import dev.amir.synapse.messaging.domain.enums.RoomType;
import java.util.Objects;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

public record RoomSummarySearchCriteria(
    UUID userId, @Nullable RoomType type, RoomStatus status, int page, int size) {
  private static final int MIN_PAGE_SIZE = 1;

  public RoomSummarySearchCriteria {
    Objects.requireNonNull(userId, "User ID cannot be null");
    Objects.requireNonNull(status, "Room status cannot be null");
    if (page < 0) {
      throw new IllegalArgumentException("Page index cannot be negative.");
    }
    if (size < MIN_PAGE_SIZE) {
      throw new IllegalArgumentException("Page size must be positive.");
    }
  }
}
