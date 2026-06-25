package dev.amir.synapse.messaging.application.command.create_direct_room;

import dev.amir.synapse.messaging.domain.model.Room;
import java.time.Instant;
import java.util.UUID;

public record CreateDirectRoomResponse(
    UUID roomId, UUID creatorId, UUID recipientId, Instant createdAt) {
  public static CreateDirectRoomResponse from(Room room, UUID creatorId, UUID recipientId) {
    return new CreateDirectRoomResponse(
        room.getId().getValue(), creatorId, recipientId, room.getCreatedAt());
  }
}
