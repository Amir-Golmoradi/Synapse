package dev.amir.synapse.messaging.domain.port.out;

import dev.amir.synapse.messaging.domain.model.Room;
import java.util.Optional;
import java.util.UUID;

public interface LoadRoomPort {
  Optional<Room> findById(UUID roomId);

  boolean hasActiveMembership(UUID roomId, UUID userId);
}
