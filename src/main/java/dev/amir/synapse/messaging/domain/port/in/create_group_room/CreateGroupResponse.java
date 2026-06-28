package dev.amir.synapse.messaging.domain.port.in.create_group_room;

import java.time.Instant;
import java.util.UUID;

public record CreateGroupResponse(
    UUID groupId, String name, String avatarUrl, int members, Instant createdAt) {}
