package dev.amir.synapse.messaging.domain.port.in.list_room_inbox;

import java.util.List;
import java.util.Objects;

public record ListRoomInboxResponse(
    List<RoomSummary> items, int page, int size, long totalElements, int totalPages) {
  private static final int MIN_PAGE_SIZE = 1;

  public ListRoomInboxResponse {
    Objects.requireNonNull(items, "Room summaries cannot be null");
    if (page < 0) {
      throw new IllegalArgumentException("Page index cannot be negative.");
    }
    if (size < MIN_PAGE_SIZE) {
      throw new IllegalArgumentException("Page size must be positive.");
    }
    if (totalElements < 0) {
      throw new IllegalArgumentException("Total elements cannot be negative.");
    }
    if (totalPages < 0) {
      throw new IllegalArgumentException("Total pages cannot be negative.");
    }
    items = List.copyOf(items);
  }
}
