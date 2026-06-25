package dev.amir.synapse.messaging.domain.port.in.create_channel_room;

import dev.amir.synapse.messaging.domain.model.Room;
import java.time.Instant;
import java.util.UUID;

public record CreateChannelResponse(
    UUID channelId, String name, String avatarUrl, int memberCount, Instant createdAt) {
  public static CreateChannelResponse from(Room room) {
    return new CreateChannelResponse(
        room.getId().getValue(),
        room.getName(),
        room.getAvatarUrl(),
        room.memberCount(),
        room.getCreatedAt());
  }
}
