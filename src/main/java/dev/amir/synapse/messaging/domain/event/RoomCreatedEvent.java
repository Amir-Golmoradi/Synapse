package dev.amir.synapse.messaging.domain.event;

import dev.amir.synapse.messaging.domain.enums.RoomType;
import dev.amir.synapse.messaging.domain.value_object.RoomId;
import dev.amir.synapse.shared.domain.DomainEvent;
import java.time.Instant;

public record RoomCreatedEvent(
    RoomId roomId, RoomType type, String name, String avatarUrl, Instant occurredOn)
    implements DomainEvent {

  public RoomCreatedEvent(RoomId roomId, RoomType type, String name, String avatarUrl) {
    this(roomId, type, name, avatarUrl, Instant.now());
  }

  @Override
  public Instant occurredOn() {
    return occurredOn;
  }
}
