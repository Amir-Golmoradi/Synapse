package dev.amir.synapse.messaging.application.command.create_direct_room.exception;

import dev.amir.synapse.messaging.domain.exception.RoomOperationException;

public class DirectMessageRecipientNotFoundException extends RoomOperationException {
  private static final long serialVersionUID = 1L;

  public DirectMessageRecipientNotFoundException() {
    super(
        "Direct message recipient not found",
        "ROOM_PARTICIPANT_NOT_FOUND",
        "Room participant not found",
        404);
  }
}
