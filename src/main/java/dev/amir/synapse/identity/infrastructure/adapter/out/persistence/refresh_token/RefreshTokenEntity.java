package dev.amir.synapse.identity.infrastructure.adapter.out.persistence.refresh_token;

import dev.amir.synapse.identity.domain.value_object.UserId;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
    name = "refresh_tokens",
    indexes = {@Index(name = "idx_refresh_tokens_token_hash", columnList = "token_hash")})
public class RefreshTokenEntity {

  @Id private UUID id;

  @Column(nullable = false)
  private UUID userId;

  @Column(name = "token_hash", nullable = false, unique = true)
  private String tokenHash;

  @Column(nullable = false)
  private Instant expiresAt;

  @Column(nullable = false)
  private boolean revoked;

  @Column(nullable = false, updatable = false)
  private Instant createdAt;

  @PrePersist
  void onCreate() {
    createdAt = Instant.now();
  }

  protected RefreshTokenEntity() {}

  private RefreshTokenEntity(
      UUID id, UUID userId, String tokenHash, Instant expiresAt, boolean revoked) {
    this.id = id;
    this.userId = userId;
    this.tokenHash = tokenHash;
    this.expiresAt = expiresAt;
    this.revoked = revoked;
    this.createdAt = Instant.now();
  }

  public static RefreshTokenEntity create(
      UUID id, UUID userId, String tokenHash, Instant expiresAt, boolean revoked) {
    return new RefreshTokenEntity(id, userId, tokenHash, expiresAt, revoked);
  }

  public static RefreshTokenEntity reconstitute(
      UUID id, UserId userId, String tokenHash, Instant expiresAt, boolean revoked) {
    return new RefreshTokenEntity(id, userId.getValue(), tokenHash, expiresAt, revoked);
  }

  public UUID getId() {
    return id;
  }

  public UUID getUserId() {
    return userId;
  }

  public boolean isRevoked() {
    return revoked;
  }

  public String getTokenHash() {
    return tokenHash;
  }

  public Instant getExpiresAt() {
    return expiresAt;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }
}
