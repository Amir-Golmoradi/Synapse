package dev.amir.synapse.messaging.domain.port.in.create_group_room;

import java.util.Set;
import java.util.UUID;

public record CreateGroupCommand(
    UUID creatorId, String name, String avatarUrl, Set<UUID> initialMemberIds) {
  public static CreateGroupCommand from(
      UUID creatorId, String name, String avatarUrl, Set<UUID> initialMemberIds) {
    return new CreateGroupCommand(creatorId, name, avatarUrl, initialMemberIds);
  }
}
