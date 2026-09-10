package dev.amir.synapse.call.application.model;

import java.time.Instant;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

public record CallRuntimeState(
    UUID callId,
    UUID callerClientInstanceId,
    @Nullable UUID calleeClientInstanceId,
    int generation,
    boolean callerReady,
    boolean calleeReady,
    boolean callerConnected,
    boolean calleeConnected,
    Instant callerLeaseExpiresAt,
    @Nullable Instant calleeLeaseExpiresAt,
    @Nullable UUID callerResumeRequestId,
    @Nullable UUID calleeResumeRequestId,
    @Nullable UUID offerMessageId,
    @Nullable String offerDigest,
    @Nullable UUID answerMessageId,
    @Nullable String answerDigest,
    int callerCandidateCount,
    int calleeCandidateCount,
    String serverInstanceId) {

  public CallRuntimeState withCallee(UUID instanceId, Instant leaseExpiry) {
    return new CallRuntimeState(
        callId,
        callerClientInstanceId,
        instanceId,
        Math.max(1, generation),
        false,
        false,
        false,
        false,
        callerLeaseExpiresAt,
        leaseExpiry,
        callerResumeRequestId,
        calleeResumeRequestId,
        null,
        null,
        null,
        null,
        0,
        0,
        serverInstanceId);
  }

  public CallRuntimeState renew(UUID userId, UUID callerId, Instant leaseExpiry) {
    return userId.equals(callerId)
        ? new CallRuntimeState(
            callId,
            callerClientInstanceId,
            calleeClientInstanceId,
            generation,
            callerReady,
            calleeReady,
            callerConnected,
            calleeConnected,
            leaseExpiry,
            calleeLeaseExpiresAt,
            callerResumeRequestId,
            calleeResumeRequestId,
            offerMessageId,
            offerDigest,
            answerMessageId,
            answerDigest,
            callerCandidateCount,
            calleeCandidateCount,
            serverInstanceId)
        : new CallRuntimeState(
            callId,
            callerClientInstanceId,
            calleeClientInstanceId,
            generation,
            callerReady,
            calleeReady,
            callerConnected,
            calleeConnected,
            callerLeaseExpiresAt,
            leaseExpiry,
            callerResumeRequestId,
            calleeResumeRequestId,
            offerMessageId,
            offerDigest,
            answerMessageId,
            answerDigest,
            callerCandidateCount,
            calleeCandidateCount,
            serverInstanceId);
  }

  public CallRuntimeState ready(UUID userId, UUID callerId) {
    return new CallRuntimeState(
        callId,
        callerClientInstanceId,
        calleeClientInstanceId,
        generation,
        callerReady || userId.equals(callerId),
        calleeReady || !userId.equals(callerId),
        callerConnected,
        calleeConnected,
        callerLeaseExpiresAt,
        calleeLeaseExpiresAt,
        callerResumeRequestId,
        calleeResumeRequestId,
        offerMessageId,
        offerDigest,
        answerMessageId,
        answerDigest,
        callerCandidateCount,
        calleeCandidateCount,
        serverInstanceId);
  }

  public CallRuntimeState connected(UUID userId, UUID callerId) {
    return new CallRuntimeState(
        callId,
        callerClientInstanceId,
        calleeClientInstanceId,
        generation,
        callerReady,
        calleeReady,
        callerConnected || userId.equals(callerId),
        calleeConnected || !userId.equals(callerId),
        callerLeaseExpiresAt,
        calleeLeaseExpiresAt,
        callerResumeRequestId,
        calleeResumeRequestId,
        offerMessageId,
        offerDigest,
        answerMessageId,
        answerDigest,
        callerCandidateCount,
        calleeCandidateCount,
        serverInstanceId);
  }

  public CallRuntimeState nextGeneration(
      UUID actor, UUID callerId, UUID requestId, String instance) {
    return new CallRuntimeState(
        callId,
        callerClientInstanceId,
        calleeClientInstanceId,
        generation + 1,
        false,
        false,
        false,
        false,
        callerLeaseExpiresAt,
        calleeLeaseExpiresAt,
        actor.equals(callerId) ? requestId : callerResumeRequestId,
        actor.equals(callerId) ? calleeResumeRequestId : requestId,
        null,
        null,
        null,
        null,
        0,
        0,
        instance);
  }

  public CallRuntimeState recordResume(UUID actor, UUID callerId, UUID requestId) {
    return new CallRuntimeState(
        callId,
        callerClientInstanceId,
        calleeClientInstanceId,
        generation,
        callerReady,
        calleeReady,
        callerConnected,
        calleeConnected,
        callerLeaseExpiresAt,
        calleeLeaseExpiresAt,
        actor.equals(callerId) ? requestId : callerResumeRequestId,
        actor.equals(callerId) ? calleeResumeRequestId : requestId,
        offerMessageId,
        offerDigest,
        answerMessageId,
        answerDigest,
        callerCandidateCount,
        calleeCandidateCount,
        serverInstanceId);
  }

  public CallRuntimeState recordOffer(UUID messageId, String digest) {
    return withSignalState(
        messageId,
        digest,
        answerMessageId,
        answerDigest,
        callerCandidateCount,
        calleeCandidateCount);
  }

  public CallRuntimeState recordAnswer(UUID messageId, String digest) {
    return withSignalState(
        offerMessageId, offerDigest, messageId, digest, callerCandidateCount, calleeCandidateCount);
  }

  public CallRuntimeState recordCandidate(boolean caller) {
    return withSignalState(
        offerMessageId,
        offerDigest,
        answerMessageId,
        answerDigest,
        callerCandidateCount + (caller ? 1 : 0),
        calleeCandidateCount + (caller ? 0 : 1));
  }

  private CallRuntimeState withSignalState(
      @Nullable UUID nextOfferMessageId,
      @Nullable String nextOfferDigest,
      @Nullable UUID nextAnswerMessageId,
      @Nullable String nextAnswerDigest,
      int nextCallerCandidateCount,
      int nextCalleeCandidateCount) {
    return new CallRuntimeState(
        callId,
        callerClientInstanceId,
        calleeClientInstanceId,
        generation,
        callerReady,
        calleeReady,
        callerConnected,
        calleeConnected,
        callerLeaseExpiresAt,
        calleeLeaseExpiresAt,
        callerResumeRequestId,
        calleeResumeRequestId,
        nextOfferMessageId,
        nextOfferDigest,
        nextAnswerMessageId,
        nextAnswerDigest,
        nextCallerCandidateCount,
        nextCalleeCandidateCount,
        serverInstanceId);
  }

  public boolean bothReady() {
    return callerReady && calleeReady;
  }

  public boolean bothConnected() {
    return callerConnected && calleeConnected;
  }
}
