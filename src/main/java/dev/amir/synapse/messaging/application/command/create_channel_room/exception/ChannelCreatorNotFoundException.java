package dev.amir.synapse.messaging.application.command.create_channel_room.exception;

public class ChannelCreatorNotFoundException extends RuntimeException {
  private static final long serialVersionUID = 1L;

  public ChannelCreatorNotFoundException() {
    super("Direct message creator not found");
  }
}
