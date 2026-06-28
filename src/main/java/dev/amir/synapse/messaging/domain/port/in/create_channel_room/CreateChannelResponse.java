package dev.amir.synapse.messaging.domain.port.in.create_channel_room;

import java.time.Instant;
import java.util.UUID;

public record CreateChannelResponse(
    UUID channelId, String name, String avatarUrl, int memberCount, Instant createdAt) {}
