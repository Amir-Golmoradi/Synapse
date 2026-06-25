package dev.amir.synapse.messaging.domain.event;

import dev.amir.synapse.messaging.domain.value_object.MemberId;
import dev.amir.synapse.messaging.domain.value_object.RoomId;
import dev.amir.synapse.shared.domain.DomainEvent;
import java.time.Instant;

public record MemberRemovedEvent(RoomId roomId, MemberId memberId, Instant occurredOn)
    implements DomainEvent {

  public MemberRemovedEvent(RoomId roomId, MemberId memberId) {
    this(roomId, memberId, Instant.now());
  }

  @Override
  public Instant occurredOn() {
    return occurredOn;
  }
}
