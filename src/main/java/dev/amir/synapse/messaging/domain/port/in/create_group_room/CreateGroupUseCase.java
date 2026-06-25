package dev.amir.synapse.messaging.domain.port.in.create_group_room;

@FunctionalInterface
public interface CreateGroupUseCase {
  CreateGroupResponse handle(CreateGroupCommand command);
}
