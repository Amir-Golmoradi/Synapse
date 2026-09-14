package dev.amir.synapse.messaging.infrastructure.adapter.in.ws.message;

import java.util.UUID;
import org.jspecify.annotations.Nullable;

public record MessageErrorResponse(
    String errorCode, String message, @Nullable UUID roomId, @Nullable UUID clientMessageId) {}
