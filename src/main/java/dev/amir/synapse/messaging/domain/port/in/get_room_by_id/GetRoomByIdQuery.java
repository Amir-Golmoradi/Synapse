package dev.amir.synapse.messaging.domain.port.in.get_room_by_id;

import java.util.Objects;
import java.util.UUID;

public record GetRoomByIdQuery(UUID userId, UUID roomId) {
  public GetRoomByIdQuery {
    Objects.requireNonNull(userId, "User ID cannot be null");
    Objects.requireNonNull(roomId, "Room ID cannot be null");
  }
}
