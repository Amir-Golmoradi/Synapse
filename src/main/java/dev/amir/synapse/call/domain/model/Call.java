package dev.amir.synapse.call.domain.model;

import dev.amir.synapse.call.domain.enums.CallStatus;
import dev.amir.synapse.call.domain.enums.CallTerminationReason;
import dev.amir.synapse.call.domain.event.CallStartedEvent;
import dev.amir.synapse.call.domain.event.CallStatusChangedEvent;
import dev.amir.synapse.call.domain.exception.CallOperationException;
import dev.amir.synapse.call.domain.value_object.CallId;
import dev.amir.synapse.call.domain.value_object.CallParticipants;
import dev.amir.synapse.shared.domain.AggregateRoot;
import dev.amir.synapse.shared.domain.DomainEvent;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

public final class Call extends AggregateRoot<CallId, DomainEvent> {
  private final CallParticipants participants;
  private final UUID clientRequestId;
  private final String startRequestFingerprint;
  private final Instant createdAt;
  private final Long version;
  private CallStatus status;
  private Instant updatedAt;
  private Instant acceptedAt;
  private Instant connectedAt;
  private Instant endedAt;
  private Instant deadlineAt;
  private CallTerminationReason terminationReason;
  private UUID terminatedBy;

  private Call(CallSnapshot snapshot) {
    super(Objects.requireNonNull(snapshot.id()));
    participants = Objects.requireNonNull(snapshot.participants());
    clientRequestId = Objects.requireNonNull(snapshot.clientRequestId());
    startRequestFingerprint = Objects.requireNonNull(snapshot.startRequestFingerprint());
    status = Objects.requireNonNull(snapshot.status());
    createdAt = Objects.requireNonNull(snapshot.createdAt());
    updatedAt = Objects.requireNonNull(snapshot.updatedAt());
    acceptedAt = snapshot.acceptedAt();
    connectedAt = snapshot.connectedAt();
    endedAt = snapshot.endedAt();
    deadlineAt = snapshot.deadlineAt();
    terminationReason = snapshot.terminationReason();
    terminatedBy = snapshot.terminatedBy();
    version = snapshot.version();
  }

  public static Call start(
      CallParticipants participants,
      UUID clientRequestId,
      String fingerprint,
      Instant now,
      Instant ringingDeadline) {
    var call =
        new Call(
            new CallSnapshot(
                CallId.generate(),
                participants,
                clientRequestId,
                fingerprint,
                CallStatus.RINGING,
                now,
                now,
                null,
                null,
                null,
                ringingDeadline,
                null,
                null,
                null));
    call.registerEvent(
        new CallStartedEvent(
            call.getId().value(), participants.callerId(), participants.calleeId(), now));
    return call;
  }

  public static Call rehydrate(CallSnapshot snapshot) {
    return new Call(snapshot);
  }

  public void accept(UUID actor, Instant now, Instant connectionDeadline) {
    requireCallee(actor);
    if (status == CallStatus.CONNECTING
        || status == CallStatus.ACTIVE
        || status == CallStatus.RECOVERING) {
      return;
    }
    require(CallStatus.RINGING);
    requireBeforeDeadline(now);
    acceptedAt = now;
    transition(CallStatus.CONNECTING, now, connectionDeadline, null, null);
  }

  public void reject(UUID actor, Instant now) {
    requireCallee(actor);
    if (status == CallStatus.REJECTED) {
      return;
    }
    require(CallStatus.RINGING);
    requireBeforeDeadline(now);
    transition(CallStatus.REJECTED, now, null, CallTerminationReason.REJECTED, actor);
  }

  public void end(UUID actor, boolean mediaError, Instant now) {
    requireParticipant(actor);
    if (status.isTerminal()) {
      return;
    }
    if (status == CallStatus.RINGING) {
      if (!participants.callerId().equals(actor)) {
        throw CallOperationException.conflict();
      }
      transition(CallStatus.CANCELLED, now, null, CallTerminationReason.CALLER_CANCELLED, actor);
      return;
    }
    transition(
        mediaError ? CallStatus.FAILED : CallStatus.ENDED,
        now,
        null,
        mediaError ? CallTerminationReason.MEDIA_ERROR : CallTerminationReason.HANGUP,
        actor);
  }

  public void markConnected(Instant now) {
    if (status == CallStatus.ACTIVE) {
      return;
    }
    if (status != CallStatus.CONNECTING && status != CallStatus.RECOVERING) {
      throw CallOperationException.conflict();
    }
    if (connectedAt == null) {
      connectedAt = now;
    }
    transition(CallStatus.ACTIVE, now, null, null, null);
  }

  public void startRecovery(Instant now, Instant recoveryDeadline) {
    if (status == CallStatus.RECOVERING) {
      return;
    }
    require(CallStatus.ACTIVE);
    transition(CallStatus.RECOVERING, now, recoveryDeadline, null, null);
  }

  public void expire(Instant now) {
    if (status.isTerminal()) {
      return;
    }
    requireDeadlineElapsed(now);
    if (status == CallStatus.RINGING) {
      transition(CallStatus.MISSED, now, null, CallTerminationReason.NO_ANSWER, null);
    } else {
      transition(CallStatus.FAILED, now, null, CallTerminationReason.CONNECTION_TIMEOUT, null);
    }
  }

  public void expireUnreachable(UUID actor, Instant now) {
    requireParticipant(actor);
    if (status.isTerminal()) {
      return;
    }
    var terminalStatus = status == CallStatus.RINGING ? CallStatus.CANCELLED : CallStatus.FAILED;
    transition(terminalStatus, now, null, CallTerminationReason.PARTICIPANT_UNREACHABLE, actor);
  }

  private void transition(
      CallStatus next,
      Instant now,
      @Nullable Instant nextDeadline,
      @Nullable CallTerminationReason reason,
      @Nullable UUID actor) {
    var previous = status;
    status = next;
    updatedAt = now;
    deadlineAt = nextDeadline;
    terminationReason = reason;
    terminatedBy = actor;
    if (next.isTerminal()) {
      endedAt = now;
    }
    registerEvent(new CallStatusChangedEvent(getId().value(), previous, next, now));
  }

  private void require(CallStatus expected) {
    if (status != expected) {
      throw CallOperationException.conflict();
    }
  }

  private void requireBeforeDeadline(Instant now) {
    if (deadlineAt != null && !now.isBefore(deadlineAt)) {
      throw CallOperationException.conflict();
    }
  }

  private void requireDeadlineElapsed(Instant now) {
    if (deadlineAt == null || now.isBefore(deadlineAt)) {
      throw CallOperationException.conflict();
    }
  }

  private void requireParticipant(UUID actor) {
    if (!participants.contains(actor)) {
      throw CallOperationException.notFound();
    }
  }

  private void requireCallee(UUID actor) {
    requireParticipant(actor);
    if (!participants.calleeId().equals(actor)) {
      throw CallOperationException.forbidden();
    }
  }

  public CallSnapshot snapshot() {
    return new CallSnapshot(
        getId(),
        participants,
        clientRequestId,
        startRequestFingerprint,
        status,
        createdAt,
        updatedAt,
        acceptedAt,
        connectedAt,
        endedAt,
        deadlineAt,
        terminationReason,
        terminatedBy,
        version);
  }

  public CallParticipants participants() {
    return participants;
  }

  public CallStatus status() {
    return status;
  }

  public UUID clientRequestId() {
    return clientRequestId;
  }

  public String startRequestFingerprint() {
    return startRequestFingerprint;
  }

  public Instant deadlineAt() {
    return deadlineAt;
  }
}
