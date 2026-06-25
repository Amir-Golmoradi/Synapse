package dev.amir.synapse.messaging.application.command.create_group_room;

import dev.amir.synapse.identity.application.api.user_lookup.UserLookupUseCase;
import dev.amir.synapse.messaging.application.command.create_group_room.exception.GroupCreatorNotFoundException;
import dev.amir.synapse.messaging.application.command.create_group_room.exception.GroupMembersNotFoundException;
import dev.amir.synapse.messaging.domain.model.Room;
import dev.amir.synapse.messaging.domain.port.in.create_group_room.CreateGroupCommand;
import dev.amir.synapse.messaging.domain.port.in.create_group_room.CreateGroupResponse;
import dev.amir.synapse.messaging.domain.port.in.create_group_room.CreateGroupUseCase;
import dev.amir.synapse.messaging.domain.port.out.SaveRoomPort;
import dev.amir.synapse.messaging.domain.value_object.MemberId;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class CreateGroupHandler implements CreateGroupUseCase {
  private final SaveRoomPort roomPort;
  private final UserLookupUseCase lookupUseCase;

  public CreateGroupHandler(SaveRoomPort roomPort, UserLookupUseCase lookupUseCase) {
    this.roomPort = roomPort;
    this.lookupUseCase = lookupUseCase;
  }

  @Override
  @Transactional
  public CreateGroupResponse handle(CreateGroupCommand command) {
    if (!lookupUseCase.existsByUserId(command.creatorId())) {
      throw new GroupCreatorNotFoundException();
    }
    var extraMembers =
        command.initialMemberIds().stream()
            .filter(id -> !id.equals(command.creatorId()))
            .collect(Collectors.toSet());

    var found = lookupUseCase.getUsersByIds(extraMembers);
    var unknownMembers =
        extraMembers.stream().filter(id -> !found.containsKey(id)).collect(Collectors.toSet());

    if (!unknownMembers.isEmpty()) {
      throw new GroupMembersNotFoundException(unknownMembers);
    }

    // ── 3. Cross the boundary: UUID → MemberId ─────────────────────────
    var creatorMemberId = MemberId.of(command.creatorId());
    var room = Room.createGroupRoom(creatorMemberId, command.name(), command.avatarUrl());

    extraMembers.stream().map(MemberId::of).forEach(room::addMember);
    roomPort.save(room);

    return CreateGroupResponse.from(room);
  }
}
