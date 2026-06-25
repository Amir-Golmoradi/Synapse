package dev.amir.synapse.messaging.application.command.create_direct_room.exception;

public class DirectMessageCreatorNotFoundException extends RuntimeException {
  private static final long serialVersionUID = 1L;

  public DirectMessageCreatorNotFoundException() {
    super("Direct message creator not found");
  }
}
