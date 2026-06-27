package dev.amir.synapse.messaging.domain.event;

import dev.amir.synapse.messaging.domain.enums.RoomRole;
import dev.amir.synapse.messaging.domain.value_object.MemberId;
import dev.amir.synapse.messaging.domain.value_object.RoomId;
import dev.amir.synapse.shared.domain.DomainEvent;
import java.time.Instant;

public record MemberRoleChangedEvent(
    RoomId roomId, MemberId targetId, RoomRole currentRole, RoomRole newRole, Instant occurredOn)
    implements DomainEvent {
  public MemberRoleChangedEvent(
      RoomId roomId, MemberId targetId, RoomRole currentRole, RoomRole newRole) {
    this(roomId, targetId, currentRole, newRole, Instant.now());
  }

  @Override
  public Instant occurredOn() {
    return occurredOn;
  }
}
