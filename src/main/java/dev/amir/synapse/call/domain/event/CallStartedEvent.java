package dev.amir.synapse.call.domain.event;

import dev.amir.synapse.call.domain.enums.CallMediaType;
import dev.amir.synapse.shared.domain.DomainEvent;
import java.time.Instant;
import java.util.UUID;

public record CallStartedEvent(
    UUID callId, UUID callerId, UUID calleeId, CallMediaType mediaType, Instant occurredOn)
    implements DomainEvent {}
