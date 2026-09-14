package dev.amir.synapse.messaging.application.command.create_direct_room.exception;

import dev.amir.synapse.messaging.domain.exception.RoomOperationException;

public class DirectMessageCreatorNotFoundException extends RoomOperationException {
  private static final long serialVersionUID = 1L;

  public DirectMessageCreatorNotFoundException() {
    super(
        "Direct message creator not found",
        "ROOM_PARTICIPANT_NOT_FOUND",
        "Room participant not found",
        404);
  }
}
