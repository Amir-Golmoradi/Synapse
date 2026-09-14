package dev.amir.synapse.messaging.infrastructure.adapter.in.ws.message;

import java.util.UUID;

public record SendMessageRequest(UUID clientMessageId, String text) {}
