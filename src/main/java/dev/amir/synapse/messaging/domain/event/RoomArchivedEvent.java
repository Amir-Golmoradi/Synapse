package dev.amir.synapse.messaging.domain.event;

import dev.amir.synapse.messaging.domain.value_object.RoomId;
import dev.amir.synapse.shared.domain.DomainEvent;
import java.time.Instant;

public record RoomArchivedEvent(RoomId roomId, Instant occurredOn) implements DomainEvent {

  public RoomArchivedEvent(RoomId roomId) {
    this(roomId, Instant.now());
  }

  @Override
  public Instant occurredOn() {
    return occurredOn;
  }
}
