package dev.amir.synapse.messaging.domain.value_object;

import dev.amir.synapse.messaging.domain.enums.RoomRole;
import dev.amir.synapse.shared.domain.ValueObject;
import java.time.Instant;

public record RoomMember(MemberId memberId, RoomRole role, Instant joinedAt)
    implements ValueObject {
  public static RoomMember create(MemberId memberId, RoomRole role, Instant joinedAt) {
    return new RoomMember(memberId, role, joinedAt);
  }

  /**
   * Returns a NEW RoomMember with a different role. The original is never mutated — value objects
   * are immutable, so a role change produces a new instance.
   */
  public RoomMember withRole(RoomRole role) {
    return new RoomMember(memberId, role, joinedAt);
  }

  public boolean isOwner() {
    return role == RoomRole.OWNER;
  }

  public RoomRole getRole() {
    return role;
  }

  public MemberId getMemberId() {
    return memberId;
  }

  public Instant getJoinedAt() {
    return joinedAt;
  }
}
