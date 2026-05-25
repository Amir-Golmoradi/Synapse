package dev.amir.synapse.identity.infrastructure.adapter.in.persistence.refresh_token;

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
    indexes = {@Index(name = "idx_refresh_tokens_token_hash", columnList = "tokenHash")})
public class RefreshTokenEntity {

  @Id UUID id;

  @Column(nullable = false)
  UUID userId;

  @Column(nullable = false, unique = true)
  String tokenHash;

  @Column(nullable = false)
  Instant expiresAt;

  @Column(nullable = false)
  boolean revoked;

  @Column(nullable = false, updatable = false)
  Instant createdAt;

  @PrePersist
  void onCreate() {
    createdAt = Instant.now();
  }
}
