package dev.amir.synapse.messaging.domain.value_object;

import dev.amir.synapse.messaging.domain.enums.RoomRole;
import dev.amir.synapse.shared.domain.ValueObject;
import java.time.Instant;
import java.util.List;

public class RoomMember extends ValueObject {
  private final MemberId memberId;
  private final RoomRole role;
  private final Instant joinedAt;

  private RoomMember(MemberId memberId, RoomRole role, Instant joinedAt) {
    this.memberId = memberId;
    this.role = role;
    this.joinedAt = joinedAt;
  }

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

  @Override
  public List<Object> getAtomicValues() {
    return List.of(memberId, role, joinedAt);
  }
}
