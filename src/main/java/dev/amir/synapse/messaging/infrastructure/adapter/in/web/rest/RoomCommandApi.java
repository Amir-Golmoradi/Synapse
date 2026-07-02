package dev.amir.synapse.messaging.infrastructure.adapter.in.web.rest;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

import dev.amir.synapse.messaging.domain.port.in.create_channel_room.CreateChannelCommand;
import dev.amir.synapse.messaging.domain.port.in.create_channel_room.CreateChannelResponse;
import dev.amir.synapse.messaging.domain.port.in.create_channel_room.CreateChannelUseCase;
import dev.amir.synapse.messaging.domain.port.in.create_direct_room.CreateDirectRoomCommand;
import dev.amir.synapse.messaging.domain.port.in.create_direct_room.CreateDirectRoomResponse;
import dev.amir.synapse.messaging.domain.port.in.create_direct_room.CreateDirectRoomUseCase;
import dev.amir.synapse.messaging.domain.port.in.create_group_room.CreateGroupCommand;
import dev.amir.synapse.messaging.domain.port.in.create_group_room.CreateGroupResponse;
import dev.amir.synapse.messaging.domain.port.in.create_group_room.CreateGroupUseCase;
import dev.amir.synapse.messaging.infrastructure.adapter.in.web.dto.request.CreateChannelRequest;
import dev.amir.synapse.messaging.infrastructure.adapter.in.web.dto.request.CreateDirectRequest;
import dev.amir.synapse.messaging.infrastructure.adapter.in.web.dto.request.CreateGroupRequest;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value = "api/v1/room", produces = APPLICATION_JSON_VALUE)
public class RoomCommandApi {
  private final CreateGroupUseCase groupUseCase;
  private final CreateChannelUseCase channelUseCase;
  private final CreateDirectRoomUseCase directRoomUseCase;

  public RoomCommandApi(
      CreateGroupUseCase groupUseCase,
      CreateChannelUseCase channelUseCase,
      CreateDirectRoomUseCase directRoomUseCase) {
    this.groupUseCase = groupUseCase;
    this.channelUseCase = channelUseCase;
    this.directRoomUseCase = directRoomUseCase;
  }

  @PostMapping("/direct")
  public ResponseEntity<CreateDirectRoomResponse> createDirectRoom(
      @Valid @RequestBody CreateDirectRequest request, Authentication authentication) {
    var command =
        CreateDirectRoomCommand.from(
            UUID.fromString(authentication.getName()), request.recipientId());
    return ResponseEntity.status(HttpStatus.CREATED).body(directRoomUseCase.handle(command));
  }

  @PostMapping("/channel")
  public ResponseEntity<CreateChannelResponse> createChannelRoom(
      @Valid @RequestBody CreateChannelRequest request, Authentication authentication) {
    var command =
        CreateChannelCommand.from(
            UUID.fromString(authentication.getName()),
            request.name(),
            request.avatarUrl(),
            request.initialMemberIds());

    return ResponseEntity.status(HttpStatus.CREATED).body(channelUseCase.handle(command));
  }

  @PostMapping("/group")
  public ResponseEntity<CreateGroupResponse> createGroupRoom(
      @Valid @RequestBody CreateGroupRequest request, Authentication authentication) {
    var command =
        CreateGroupCommand.from(
            UUID.fromString(authentication.getName()),
            request.name(),
            request.avatarUrl(),
            request.initialMemberIds());
    return ResponseEntity.status(HttpStatus.CREATED).body(groupUseCase.handle(command));
  }
}
