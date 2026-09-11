package dev.amir.synapse.call.infrastructure.adapter.out.persistence;

import dev.amir.synapse.call.domain.enums.CallStatus;
import dev.amir.synapse.call.domain.enums.CallTerminationReason;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "calls")
class CallJpaEntity {
  @Id private UUID id;

  @Column(name = "caller_id", nullable = false, updatable = false)
  private UUID callerId;

  @Column(name = "callee_id", nullable = false, updatable = false)
  private UUID calleeId;

  @Column(name = "client_request_id", nullable = false, updatable = false)
  private UUID clientRequestId;

  @Column(name = "start_request_fingerprint", nullable = false, updatable = false, length = 64)
  private String startRequestFingerprint;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private CallStatus status;

  @Column(
      name = "created_at",
      nullable = false,
      updatable = false,
      columnDefinition = "TIMESTAMPTZ")
  private Instant createdAt;

  @Column(name = "updated_at", nullable = false, columnDefinition = "TIMESTAMPTZ")
  private Instant updatedAt;

  @Column(name = "accepted_at", columnDefinition = "TIMESTAMPTZ")
  private Instant acceptedAt;

  @Column(name = "connected_at", columnDefinition = "TIMESTAMPTZ")
  private Instant connectedAt;

  @Column(name = "ended_at", columnDefinition = "TIMESTAMPTZ")
  private Instant endedAt;

  @Column(name = "deadline_at", columnDefinition = "TIMESTAMPTZ")
  private Instant deadlineAt;

  @Enumerated(EnumType.STRING)
  @Column(name = "termination_reason", length = 32)
  private CallTerminationReason terminationReason;

  @Column(name = "terminated_by")
  private UUID terminatedBy;

  @Version
  @Column(nullable = false)
  private Long version;

  protected CallJpaEntity() {}

  CallJpaEntity(
      UUID id,
      UUID callerId,
      UUID calleeId,
      UUID clientRequestId,
      String startRequestFingerprint,
      CallStatus status,
      Instant createdAt,
      Instant updatedAt,
      Instant acceptedAt,
      Instant connectedAt,
      Instant endedAt,
      Instant deadlineAt,
      CallTerminationReason terminationReason,
      UUID terminatedBy,
      Long version) {
    this.id = id;
    this.callerId = callerId;
    this.calleeId = calleeId;
    this.clientRequestId = clientRequestId;
    this.startRequestFingerprint = startRequestFingerprint;
    this.status = status;
    this.createdAt = createdAt;
    this.updatedAt = updatedAt;
    this.acceptedAt = acceptedAt;
    this.connectedAt = connectedAt;
    this.endedAt = endedAt;
    this.deadlineAt = deadlineAt;
    this.terminationReason = terminationReason;
    this.terminatedBy = terminatedBy;
    this.version = version;
  }

  UUID id() {
    return id;
  }

  UUID callerId() {
    return callerId;
  }

  UUID calleeId() {
    return calleeId;
  }

  UUID clientRequestId() {
    return clientRequestId;
  }

  String startRequestFingerprint() {
    return startRequestFingerprint;
  }

  CallStatus status() {
    return status;
  }

  Instant createdAt() {
    return createdAt;
  }

  Instant updatedAt() {
    return updatedAt;
  }

  Instant acceptedAt() {
    return acceptedAt;
  }

  Instant connectedAt() {
    return connectedAt;
  }

  Instant endedAt() {
    return endedAt;
  }

  Instant deadlineAt() {
    return deadlineAt;
  }

  CallTerminationReason terminationReason() {
    return terminationReason;
  }

  UUID terminatedBy() {
    return terminatedBy;
  }

  Long version() {
    return version;
  }
}
