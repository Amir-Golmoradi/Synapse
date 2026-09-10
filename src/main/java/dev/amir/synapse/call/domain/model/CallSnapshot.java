package dev.amir.synapse.call.domain.model;

import dev.amir.synapse.call.domain.enums.CallStatus;
import dev.amir.synapse.call.domain.enums.CallTerminationReason;
import dev.amir.synapse.call.domain.value_object.CallId;
import dev.amir.synapse.call.domain.value_object.CallParticipants;
import java.time.Instant;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

public record CallSnapshot(
    CallId id,
    CallParticipants participants,
    UUID clientRequestId,
    String startRequestFingerprint,
    CallStatus status,
    Instant createdAt,
    Instant updatedAt,
    @Nullable Instant acceptedAt,
    @Nullable Instant connectedAt,
    @Nullable Instant endedAt,
    @Nullable Instant deadlineAt,
    @Nullable CallTerminationReason terminationReason,
    @Nullable UUID terminatedBy,
    @Nullable Long version) {}
