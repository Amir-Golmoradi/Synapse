package dev.amir.synapse.messaging.domain.port.in.get_room_by_id;

import dev.amir.synapse.messaging.domain.port.in.list_room_inbox.RoomSummary;
import java.util.Optional;

@FunctionalInterface
public interface GetRoomByIdUseCase {
  Optional<RoomSummary> handle(GetRoomByIdQuery query);
}
