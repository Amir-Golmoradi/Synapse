package dev.amir.synapse.call.domain.port.in;

import dev.amir.synapse.call.domain.enums.CallStatus;
import dev.amir.synapse.call.domain.enums.CallTerminationReason;
import dev.amir.synapse.call.domain.model.Call;
import java.time.Instant;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

public record CallView(
    UUID callId,
    UUID callerId,
    UUID calleeId,
    CallStatus status,
    long version,
    Instant createdAt,
    Instant updatedAt,
    @Nullable Instant acceptedAt,
    @Nullable Instant connectedAt,
    @Nullable Instant endedAt,
    @Nullable Instant deadlineAt,
    @Nullable CallTerminationReason terminationReason,
    @Nullable UUID terminatedBy,
    int negotiationGeneration) {

  public static CallView from(Call call, int generation) {
    var snapshot = call.snapshot();
    return new CallView(
        snapshot.id().value(),
        snapshot.participants().callerId(),
        snapshot.participants().calleeId(),
        snapshot.status(),
        snapshot.version() == null ? 0 : snapshot.version(),
        snapshot.createdAt(),
        snapshot.updatedAt(),
        snapshot.acceptedAt(),
        snapshot.connectedAt(),
        snapshot.endedAt(),
        snapshot.deadlineAt(),
        snapshot.terminationReason(),
        snapshot.terminatedBy(),
        generation);
  }
}
