package dev.amir.synapse.messaging.domain.port.in.create_direct_room;

@FunctionalInterface
public interface CreateDirectRoomUseCase {
  CreateDirectRoomResponse handle(CreateDirectRoomCommand command);
}
