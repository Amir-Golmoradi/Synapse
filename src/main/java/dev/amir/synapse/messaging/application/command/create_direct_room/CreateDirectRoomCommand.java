package dev.amir.synapse.messaging.application.command.create_direct_room;

import java.util.UUID;

public record CreateDirectRoomCommand(UUID creatorId, UUID recipientId) {
  public static CreateDirectRoomCommand from(UUID creatorId, UUID recipientId) {
    return new CreateDirectRoomCommand(creatorId, recipientId);
  }
}
