package dev.amir.synapse.messaging.application.command.create_group_room;

import dev.amir.synapse.messaging.domain.model.Room;
import java.time.Instant;
import java.util.UUID;

public record CreateGroupResponse(
    UUID groupId, String name, String avatarUrl, int members, Instant createdAt) {
  public static CreateGroupResponse from(Room room) {
    return new CreateGroupResponse(
        room.getId().getValue(),
        room.getName(),
        room.getAvatarUrl(),
        room.memberCount(),
        room.getCreatedAt());
  }
}
