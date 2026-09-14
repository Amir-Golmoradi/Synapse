package dev.amir.synapse.messaging.application.command.create_channel_room.exception;

import dev.amir.synapse.messaging.domain.exception.RoomOperationException;
import java.util.Set;
import java.util.UUID;

public class ChannelMembersNotFoundException extends RoomOperationException {
  private static final long serialVersionUID = 1L;

  public ChannelMembersNotFoundException(Set<UUID> missingMemberIds) {
    super(
        "The following channel members were not found: " + missingMemberIds,
        "ROOM_PARTICIPANT_NOT_FOUND",
        "Room participant not found",
        404);
  }
}
