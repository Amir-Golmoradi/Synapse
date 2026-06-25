package dev.amir.synapse.messaging.application.port.in;

import dev.amir.synapse.messaging.application.command.create_direct_room.CreateDirectRoomCommand;
import dev.amir.synapse.messaging.application.command.create_direct_room.CreateDirectRoomResponse;

@FunctionalInterface
public interface CreateDirectRoomUseCase {
  CreateDirectRoomResponse handle(CreateDirectRoomCommand command);
}
