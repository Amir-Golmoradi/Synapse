package dev.amir.synapse.messaging.application.command.create_group_room.exception;

import dev.amir.synapse.messaging.domain.exception.RoomOperationException;

public class GroupCreatorNotFoundException extends RoomOperationException {
  private static final long serialVersionUID = 1L;

  public GroupCreatorNotFoundException() {
    super(
        "Group creator not found", "ROOM_PARTICIPANT_NOT_FOUND", "Room participant not found", 404);
  }
}
