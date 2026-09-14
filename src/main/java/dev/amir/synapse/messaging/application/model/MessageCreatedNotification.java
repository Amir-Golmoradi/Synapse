package dev.amir.synapse.messaging.application.model;

import dev.amir.synapse.messaging.domain.port.in.message.MessageView;
import java.time.Instant;
import java.util.UUID;

public record MessageCreatedNotification(UUID eventId, MessageView message, Instant occurredAt) {}
