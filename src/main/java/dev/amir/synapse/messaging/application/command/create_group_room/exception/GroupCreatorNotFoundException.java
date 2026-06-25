package dev.amir.synapse.messaging.application.command.create_group_room.exception;

public class GroupCreatorNotFoundException extends RuntimeException {
  private static final long serialVersionUID = 1L;

  public GroupCreatorNotFoundException() {
    super("Group creator not found");
  }
}
