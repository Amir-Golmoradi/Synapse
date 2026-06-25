package dev.amir.synapse.messaging.domain.port.out;

import dev.amir.synapse.messaging.domain.model.Room;

@FunctionalInterface
public interface SaveRoomPort {
  Room save(Room room);
}
