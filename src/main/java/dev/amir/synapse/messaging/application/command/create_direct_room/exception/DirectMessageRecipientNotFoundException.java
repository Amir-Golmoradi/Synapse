package dev.amir.synapse.messaging.application.command.create_direct_room.exception;

public class DirectMessageRecipientNotFoundException extends RuntimeException {
  private static final long serialVersionUID = 1L;

  public DirectMessageRecipientNotFoundException() {
    super("Direct message recipient not found");
  }
}
