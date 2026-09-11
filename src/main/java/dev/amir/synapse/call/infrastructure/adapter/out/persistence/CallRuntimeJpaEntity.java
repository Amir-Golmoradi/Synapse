package dev.amir.synapse.call.infrastructure.adapter.out.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "call_runtime")
class CallRuntimeJpaEntity {
  @Id
  @Column(name = "call_id")
  private UUID callId;

  @Column(name = "caller_client_instance_id", nullable = false)
  private UUID callerClientInstanceId;

  @Column(name = "callee_client_instance_id")
  private UUID calleeClientInstanceId;

  @Column(nullable = false)
  private int generation;

  @Column(name = "caller_ready", nullable = false)
  private boolean callerReady;

  @Column(name = "callee_ready", nullable = false)
  private boolean calleeReady;

  @Column(name = "caller_connected", nullable = false)
  private boolean callerConnected;

  @Column(name = "callee_connected", nullable = false)
  private boolean calleeConnected;

  @Column(name = "caller_lease_expires_at", nullable = false)
  private Instant callerLeaseExpiresAt;

  @Column(name = "callee_lease_expires_at")
  private Instant calleeLeaseExpiresAt;

  @Column(name = "caller_resume_request_id")
  private UUID callerResumeRequestId;

  @Column(name = "callee_resume_request_id")
  private UUID calleeResumeRequestId;

  @Column(name = "offer_message_id")
  private UUID offerMessageId;

  @Column(name = "offer_digest", length = 64)
  private String offerDigest;

  @Column(name = "answer_message_id")
  private UUID answerMessageId;

  @Column(name = "answer_digest", length = 64)
  private String answerDigest;

  @Column(name = "caller_candidate_count", nullable = false)
  private int callerCandidateCount;

  @Column(name = "callee_candidate_count", nullable = false)
  private int calleeCandidateCount;

  @Column(name = "server_instance_id", nullable = false, length = 64)
  private String serverInstanceId;

  protected CallRuntimeJpaEntity() {}

  CallRuntimeJpaEntity(
      UUID callId,
      UUID callerClientInstanceId,
      UUID calleeClientInstanceId,
      int generation,
      boolean callerReady,
      boolean calleeReady,
      boolean callerConnected,
      boolean calleeConnected,
      Instant callerLeaseExpiresAt,
      Instant calleeLeaseExpiresAt,
      UUID callerResumeRequestId,
      UUID calleeResumeRequestId,
      UUID offerMessageId,
      String offerDigest,
      UUID answerMessageId,
      String answerDigest,
      int callerCandidateCount,
      int calleeCandidateCount,
      String serverInstanceId) {
    this.callId = callId;
    this.callerClientInstanceId = callerClientInstanceId;
    this.calleeClientInstanceId = calleeClientInstanceId;
    this.generation = generation;
    this.callerReady = callerReady;
    this.calleeReady = calleeReady;
    this.callerConnected = callerConnected;
    this.calleeConnected = calleeConnected;
    this.callerLeaseExpiresAt = callerLeaseExpiresAt;
    this.calleeLeaseExpiresAt = calleeLeaseExpiresAt;
    this.callerResumeRequestId = callerResumeRequestId;
    this.calleeResumeRequestId = calleeResumeRequestId;
    this.offerMessageId = offerMessageId;
    this.offerDigest = offerDigest;
    this.answerMessageId = answerMessageId;
    this.answerDigest = answerDigest;
    this.callerCandidateCount = callerCandidateCount;
    this.calleeCandidateCount = calleeCandidateCount;
    this.serverInstanceId = serverInstanceId;
  }

  UUID callId() {
    return callId;
  }

  UUID callerClientInstanceId() {
    return callerClientInstanceId;
  }

  UUID calleeClientInstanceId() {
    return calleeClientInstanceId;
  }

  int generation() {
    return generation;
  }

  boolean callerReady() {
    return callerReady;
  }

  boolean calleeReady() {
    return calleeReady;
  }

  boolean callerConnected() {
    return callerConnected;
  }

  boolean calleeConnected() {
    return calleeConnected;
  }

  Instant callerLeaseExpiresAt() {
    return callerLeaseExpiresAt;
  }

  Instant calleeLeaseExpiresAt() {
    return calleeLeaseExpiresAt;
  }

  UUID callerResumeRequestId() {
    return callerResumeRequestId;
  }

  UUID calleeResumeRequestId() {
    return calleeResumeRequestId;
  }

  UUID offerMessageId() {
    return offerMessageId;
  }

  String offerDigest() {
    return offerDigest;
  }

  UUID answerMessageId() {
    return answerMessageId;
  }

  String answerDigest() {
    return answerDigest;
  }

  int callerCandidateCount() {
    return callerCandidateCount;
  }

  int calleeCandidateCount() {
    return calleeCandidateCount;
  }

  String serverInstanceId() {
    return serverInstanceId;
  }
}
