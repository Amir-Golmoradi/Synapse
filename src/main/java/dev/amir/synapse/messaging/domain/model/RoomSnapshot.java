package dev.amir.synapse.messaging.domain.model;

import dev.amir.synapse.messaging.domain.enums.RoomStatus;
import dev.amir.synapse.messaging.domain.enums.RoomType;
import dev.amir.synapse.messaging.domain.value_object.MemberId;
import dev.amir.synapse.messaging.domain.value_object.RoomId;
import java.time.Instant;
import java.util.Set;

public record RoomSnapshot(
    RoomId id,
    RoomType roomType,
    String name,
    String avatarUrl,
    RoomStatus status,
    Instant createdAt,
    Instant lastMessagesAt,
    Set<MemberId> initialMembers) {}
