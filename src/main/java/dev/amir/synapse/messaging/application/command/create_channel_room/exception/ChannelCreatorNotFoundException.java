package dev.amir.synapse.messaging.application.command.create_channel_room.exception;

import dev.amir.synapse.messaging.domain.exception.RoomOperationException;

public class ChannelCreatorNotFoundException extends RoomOperationException {
  private static final long serialVersionUID = 1L;

  public ChannelCreatorNotFoundException() {
    super(
        "Channel creator not found",
        "ROOM_PARTICIPANT_NOT_FOUND",
        "Room participant not found",
        404);
  }
}
