package dev.amir.synapse.call.application.model;

import dev.amir.synapse.call.domain.port.in.CallView;
import java.time.Instant;
import java.util.UUID;

public record CallLifecycleNotification(
    UUID eventId, String type, CallView call, Instant occurredAt) {}
