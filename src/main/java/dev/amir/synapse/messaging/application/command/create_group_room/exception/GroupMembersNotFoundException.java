package dev.amir.synapse.messaging.application.command.create_group_room.exception;

import java.util.Set;
import java.util.UUID;

public class GroupMembersNotFoundException extends RuntimeException {
  private static final long serialVersionUID = 1L;

  public GroupMembersNotFoundException(Set<UUID> missingMemberIds) {
    super("The following group members were not found: " + missingMemberIds);
  }
}
