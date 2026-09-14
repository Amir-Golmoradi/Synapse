package dev.amir.synapse.messaging.application.command.create_direct_room.exception;

import dev.amir.synapse.messaging.domain.exception.RoomOperationException;

public class CannotCreateDirectMessageWithSelfException extends RoomOperationException {
  private static final long serialVersionUID = 1L;

  public CannotCreateDirectMessageWithSelfException() {
    super(
        "Cannot create a direct message with self",
        "DIRECT_MESSAGE_WITH_SELF",
        "Direct message is invalid",
        400);
  }
}
