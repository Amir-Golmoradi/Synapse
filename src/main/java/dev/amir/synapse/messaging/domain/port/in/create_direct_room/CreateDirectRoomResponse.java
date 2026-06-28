package dev.amir.synapse.messaging.domain.port.in.create_direct_room;

import java.time.Instant;
import java.util.UUID;

public record CreateDirectRoomResponse(
    UUID roomId, UUID creatorId, UUID recipientId, Instant createdAt) {}
