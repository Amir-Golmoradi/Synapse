package dev.amir.synapse.call.infrastructure.adapter.in.ws;

import java.time.Instant;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

public record CallStompResponse(
    String type,
    @Nullable UUID callId,
    @Nullable UUID clientInstanceId,
    @Nullable Integer generation,
    Instant serverTime) {}
