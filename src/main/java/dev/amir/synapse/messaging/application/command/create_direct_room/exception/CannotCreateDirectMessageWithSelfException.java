package dev.amir.synapse.messaging.application.command.create_direct_room.exception;

public class CannotCreateDirectMessageWithSelfException extends RuntimeException {
  private static final long serialVersionUID = 1L;

  public CannotCreateDirectMessageWithSelfException() {
    super("Cannot create a direct message with self");
  }
}
