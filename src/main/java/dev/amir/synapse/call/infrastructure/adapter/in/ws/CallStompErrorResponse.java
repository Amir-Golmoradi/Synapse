package dev.amir.synapse.call.infrastructure.adapter.in.ws;

import java.util.UUID;
import org.jspecify.annotations.Nullable;

public record CallStompErrorResponse(String errorCode, String message, @Nullable UUID callId) {}
