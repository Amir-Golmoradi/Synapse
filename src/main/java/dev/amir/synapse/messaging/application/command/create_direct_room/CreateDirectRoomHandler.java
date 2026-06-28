package dev.amir.synapse.messaging.application.command.create_direct_room;

import dev.amir.synapse.identity.application.api.user_lookup.UserLookupUseCase;
import dev.amir.synapse.messaging.application.command.create_direct_room.exception.CannotCreateDirectMessageWithSelfException;
import dev.amir.synapse.messaging.application.command.create_direct_room.exception.DirectMessageCreatorNotFoundException;
import dev.amir.synapse.messaging.application.command.create_direct_room.exception.DirectMessageRecipientNotFoundException;
import dev.amir.synapse.messaging.domain.model.Room;
import dev.amir.synapse.messaging.domain.port.in.create_direct_room.CreateDirectRoomCommand;
import dev.amir.synapse.messaging.domain.port.in.create_direct_room.CreateDirectRoomResponse;
import dev.amir.synapse.messaging.domain.port.in.create_direct_room.CreateDirectRoomUseCase;
import dev.amir.synapse.messaging.domain.port.out.SaveRoomPort;
import dev.amir.synapse.messaging.domain.value_object.MemberId;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class CreateDirectRoomHandler implements CreateDirectRoomUseCase {
  private final SaveRoomPort saveRoomPort;
  private final UserLookupUseCase lookupUseCase;

  public CreateDirectRoomHandler(SaveRoomPort saveRoomPort, UserLookupUseCase lookupUseCase) {
    this.saveRoomPort = saveRoomPort;
    this.lookupUseCase = lookupUseCase;
  }

  @Override
  @Transactional
  public CreateDirectRoomResponse handle(CreateDirectRoomCommand command) {
    validateDirectMessage(command);

    // ── 3. Cross the boundary: UUID → MemberId ────────────────────────
    var creatorId = MemberId.of(command.creatorId());
    var recipientId = MemberId.of(command.recipientId());

    var room = Room.createDirectRoom(creatorId, recipientId);

    var savedRoom = saveRoomPort.save(room);

    return new CreateDirectRoomResponse(
        savedRoom.getId().getValue(),
        command.creatorId(),
        command.recipientId(),
        savedRoom.getCreatedAt());
  }

  private void validateDirectMessage(CreateDirectRoomCommand command) {
    if (command.creatorId().equals(command.recipientId())) {
      throw new CannotCreateDirectMessageWithSelfException();
    }

    if (!lookupUseCase.existsByUserId(command.creatorId())) {
      throw new DirectMessageCreatorNotFoundException();
    }
    if (!lookupUseCase.existsByUserId(command.recipientId())) {
      throw new DirectMessageRecipientNotFoundException();
    }
  }
}
