package dev.amir.synapse.messaging.application.command.create_channel_room;

import java.util.Set;
import java.util.UUID;

public record CreateChannelCommand(
    UUID creatorId,
    String name,
    String avatarUrl,
    Set<UUID> initialMemberIds // Can be empty but NOT NULL.
    ) {
  public static CreateChannelCommand from(
      UUID creatorId, String name, String avatarUrl, Set<UUID> initialMemberIds) {
    return new CreateChannelCommand(creatorId, name, avatarUrl, initialMemberIds);
  }
}
