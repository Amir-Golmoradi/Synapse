package dev.amir.synapse.messaging.domain.port.out;

import dev.amir.synapse.messaging.domain.model.Room;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface LoadRoomPort {
  Optional<Room> findById(UUID roomId);

  List<Room> findActiveRoomsForUser(UUID userId);

  boolean hasActiveMembership(UUID roomId, UUID userId);
}
