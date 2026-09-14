package dev.amir.synapse.messaging.domain.event;

import dev.amir.synapse.messaging.domain.value_object.MemberId;
import dev.amir.synapse.messaging.domain.value_object.RoomId;
import dev.amir.synapse.shared.domain.DomainEvent;
import java.time.Instant;
import java.util.Objects;
import java.util.Set;

public record MembersAddedEvent(RoomId roomId, Set<MemberId> memberIds, Instant occurredOn)
    implements DomainEvent {

  public MembersAddedEvent {
    Objects.requireNonNull(roomId, "Room ID cannot be null");
    Objects.requireNonNull(memberIds, "Member IDs cannot be null");
    Objects.requireNonNull(occurredOn, "Occurred timestamp cannot be null");
    if (memberIds.isEmpty()) {
      throw new IllegalArgumentException("Member IDs cannot be empty.");
    }
    memberIds = Set.copyOf(memberIds);
  }

  public MembersAddedEvent(RoomId roomId, Set<MemberId> memberIds) {
    this(roomId, memberIds, Instant.now());
  }

  @Override
  public Instant occurredOn() {
    return occurredOn;
  }
}
