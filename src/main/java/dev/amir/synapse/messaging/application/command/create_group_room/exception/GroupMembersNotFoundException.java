package dev.amir.synapse.messaging.application.command.create_group_room.exception;

import dev.amir.synapse.messaging.domain.exception.RoomOperationException;
import java.util.Set;
import java.util.UUID;

public class GroupMembersNotFoundException extends RoomOperationException {
  private static final long serialVersionUID = 1L;

  public GroupMembersNotFoundException(Set<UUID> missingMemberIds) {
    super(
        "The following group members were not found: " + missingMemberIds,
        "ROOM_PARTICIPANT_NOT_FOUND",
        "Room participant not found",
        404);
  }
}
