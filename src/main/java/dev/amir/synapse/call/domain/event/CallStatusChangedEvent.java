package dev.amir.synapse.call.domain.event;

import dev.amir.synapse.call.domain.enums.CallStatus;
import dev.amir.synapse.shared.domain.DomainEvent;
import java.time.Instant;
import java.util.UUID;

public record CallStatusChangedEvent(
    UUID callId, CallStatus previousStatus, CallStatus status, Instant occurredOn)
    implements DomainEvent {}
