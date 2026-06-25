package dev.amir.synapse.messaging.application.port.in;

import dev.amir.synapse.messaging.application.command.create_group_room.CreateGroupCommand;
import dev.amir.synapse.messaging.application.command.create_group_room.CreateGroupResponse;

@FunctionalInterface
public interface CreateGroupUseCase {
  CreateGroupResponse handle(CreateGroupCommand command);
}
