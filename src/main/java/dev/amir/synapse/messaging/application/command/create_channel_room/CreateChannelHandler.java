package dev.amir.synapse.messaging.application.command.create_channel_room;

import dev.amir.synapse.identity.application.api.user_lookup.UserLookupUseCase;
import dev.amir.synapse.messaging.application.command.create_channel_room.exception.ChannelCreatorNotFoundException;
import dev.amir.synapse.messaging.application.command.create_channel_room.exception.ChannelMembersNotFoundException;
import dev.amir.synapse.messaging.domain.model.Room;
import dev.amir.synapse.messaging.domain.port.in.create_channel_room.CreateChannelCommand;
import dev.amir.synapse.messaging.domain.port.in.create_channel_room.CreateChannelResponse;
import dev.amir.synapse.messaging.domain.port.in.create_channel_room.CreateChannelUseCase;
import dev.amir.synapse.messaging.domain.port.out.SaveRoomPort;
import dev.amir.synapse.messaging.domain.value_object.MemberId;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class CreateChannelHandler implements CreateChannelUseCase {
  private final SaveRoomPort roomPort;
  private final UserLookupUseCase lookupUseCase;

  public CreateChannelHandler(SaveRoomPort roomPort, UserLookupUseCase lookupUseCase) {
    this.roomPort = roomPort;
    this.lookupUseCase = lookupUseCase;
  }

  @Override
  @Transactional
  public CreateChannelResponse handle(CreateChannelCommand command) {
    if (!lookupUseCase.existsByUserId(command.creatorId())) {
      throw new ChannelCreatorNotFoundException();
    }

    // ── 2. Validate all initial members up front (no mutations yet) ────
    var extraMembers =
        command.initialMemberIds().stream()
            .filter(id -> !id.equals(command.creatorId()))
            .collect(Collectors.toSet());

    var found = lookupUseCase.getUsersByIds(extraMembers);
    var unknownMembers =
        extraMembers.stream().filter(id -> !found.containsKey(id)).collect(Collectors.toSet());

    if (!unknownMembers.isEmpty()) {
      throw new ChannelMembersNotFoundException(unknownMembers);
    }

    // ── 3. Cross the boundary: UUID → MemberId ─────────────────────────
    var creatorMemberId = MemberId.of(command.creatorId());

    var room = Room.createChannel(creatorMemberId, command.name(), command.avatarUrl());

    room.addMembers(extraMembers.stream().map(MemberId::of).collect(Collectors.toSet()));
    roomPort.save(room);

    return new CreateChannelResponse(
        room.getId().getValue(),
        room.getName(),
        room.getAvatarUrl(),
        room.memberCount(),
        room.getCreatedAt());
  }
}
