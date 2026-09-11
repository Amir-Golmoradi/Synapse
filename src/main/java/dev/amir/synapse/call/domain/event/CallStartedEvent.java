package dev.amir.synapse.call.domain.event;

import dev.amir.synapse.shared.domain.DomainEvent;
import java.time.Instant;
import java.util.UUID;

public record CallStartedEvent(UUID callId, UUID callerId, UUID calleeId, Instant occurredOn)
    implements DomainEvent {}
