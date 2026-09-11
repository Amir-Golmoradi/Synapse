package dev.amir.synapse.call.infrastructure.adapter.out.persistence;

import dev.amir.synapse.call.application.model.CallRuntimeState;
import dev.amir.synapse.call.domain.model.Call;
import dev.amir.synapse.call.domain.model.CallSnapshot;
import dev.amir.synapse.call.domain.value_object.CallId;
import dev.amir.synapse.call.domain.value_object.CallParticipants;
import org.springframework.stereotype.Component;

@Component
class CallPersistenceMapper {
  Call toDomain(CallJpaEntity entity) {
    return Call.rehydrate(
        new CallSnapshot(
            CallId.of(entity.id()),
            new CallParticipants(entity.callerId(), entity.calleeId()),
            entity.clientRequestId(),
            entity.startRequestFingerprint(),
            entity.status(),
            entity.createdAt(),
            entity.updatedAt(),
            entity.acceptedAt(),
            entity.connectedAt(),
            entity.endedAt(),
            entity.deadlineAt(),
            entity.terminationReason(),
            entity.terminatedBy(),
            entity.version()));
  }

  CallJpaEntity toEntity(Call call) {
    var s = call.snapshot();
    return new CallJpaEntity(
        s.id().value(),
        s.participants().callerId(),
        s.participants().calleeId(),
        s.clientRequestId(),
        s.startRequestFingerprint(),
        s.status(),
        s.createdAt(),
        s.updatedAt(),
        s.acceptedAt(),
        s.connectedAt(),
        s.endedAt(),
        s.deadlineAt(),
        s.terminationReason(),
        s.terminatedBy(),
        s.version());
  }

  CallRuntimeState toDomain(CallRuntimeJpaEntity entity) {
    return new CallRuntimeState(
        entity.callId(),
        entity.callerClientInstanceId(),
        entity.calleeClientInstanceId(),
        entity.generation(),
        entity.callerReady(),
        entity.calleeReady(),
        entity.callerConnected(),
        entity.calleeConnected(),
        entity.callerLeaseExpiresAt(),
        entity.calleeLeaseExpiresAt(),
        entity.callerResumeRequestId(),
        entity.calleeResumeRequestId(),
        entity.offerMessageId(),
        entity.offerDigest(),
        entity.answerMessageId(),
        entity.answerDigest(),
        entity.callerCandidateCount(),
        entity.calleeCandidateCount(),
        entity.serverInstanceId());
  }

  CallRuntimeJpaEntity toEntity(CallRuntimeState state) {
    return new CallRuntimeJpaEntity(
        state.callId(),
        state.callerClientInstanceId(),
        state.calleeClientInstanceId(),
        state.generation(),
        state.callerReady(),
        state.calleeReady(),
        state.callerConnected(),
        state.calleeConnected(),
        state.callerLeaseExpiresAt(),
        state.calleeLeaseExpiresAt(),
        state.callerResumeRequestId(),
        state.calleeResumeRequestId(),
        state.offerMessageId(),
        state.offerDigest(),
        state.answerMessageId(),
        state.answerDigest(),
        state.callerCandidateCount(),
        state.calleeCandidateCount(),
        state.serverInstanceId());
  }
}
