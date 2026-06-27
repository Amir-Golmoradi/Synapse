package dev.amir.synapse.messaging.infrastructure.adapter.out.persistence.model;

import dev.amir.synapse.messaging.domain.enums.RoomRole;
import dev.amir.synapse.messaging.domain.value_object.MemberId;
import dev.amir.synapse.messaging.domain.value_object.RoomMember;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import java.time.Instant;
import java.util.UUID;

@Embeddable
public record RoomMemberEmbeddable(
    @Column(name = "user_id", nullable = false, columnDefinition = "uuid") UUID userId,
    @Enumerated(EnumType.STRING) @Column(name = "role", nullable = false, length = 20)
        RoomRole role,
    @Column(name = "joined_at", nullable = false, columnDefinition = "TIMESTAMPTZ")
        Instant joinedAt) {

  public static RoomMemberEmbeddable fromDomain(RoomMember member) {
    return new RoomMemberEmbeddable(
        member.getMemberId().getValue(), member.getRole(), member.getJoinedAt());
  }

  public RoomMember toDomain() {
    return RoomMember.create(MemberId.of(userId), role, joinedAt);
  }
}
