package dev.amir.synapse.messaging.infrastructure.adapter.in.web.dto.response;

import dev.amir.synapse.messaging.domain.model.Room;
import java.time.Instant;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

public record CreateGroupResponse(
    UUID roomId,
    String name,
    @Nullable String avatarUrl,
    int joinedMembersCount,
    Instant createdAt) {
  public static CreateGroupResponse from(Room room) {
    return new CreateGroupResponse(
        room.getId().getValue(),
        room.getName(),
        room.getAvatarUrl(),
        room.memberCount(),
        room.getCreatedAt());
  }
}
