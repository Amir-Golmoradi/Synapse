package dev.amir.synapse.messaging.domain.port.out;

import java.util.Optional;
import java.util.UUID;

@FunctionalInterface
public interface LoadRoomSummaryPort {
  Optional<RoomSummaryProjection> findRoomSummaryByIdForMember(UUID roomId, UUID userId);
}
