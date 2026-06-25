package dev.amir.synapse.identity.domain.entity;

import dev.amir.synapse.identity.domain.exception.InvalidRefreshTokenException;
import dev.amir.synapse.identity.domain.value_object.UserId;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;
import java.util.UUID;

public class RefreshToken {
  private final UUID id;
  private final UserId userId;
  private final String tokenHash;
  private final Instant expiresAt;
  private boolean revoked;

  private RefreshToken(
      UUID id, UserId userId, String tokenHash, Instant expiresAt, boolean revoked) {
    this.id = id;
    this.userId = userId;
    this.tokenHash = tokenHash;
    this.expiresAt = expiresAt;
    this.revoked = revoked;
  }

  // ── Factory: issue a brand-new refresh token ────────────────────────────
  // Returns the raw UUID to hand to the client — only the hash is persisted
  public static IssueResult issue(UserId userId, Duration validity) {
    var rawToken = UUID.randomUUID().toString();
    var expirationDate = Instant.now().plus(validity);
    var refreshToken =
        new RefreshToken(UUID.randomUUID(), userId, hash(rawToken), expirationDate, false);
    return new IssueResult(refreshToken, rawToken);
  }

  // ── Factory: reconstitute from persistence ──────────────────────────────
  public static RefreshToken reconstitute(
      UUID id, UserId userId, String tokenHash, Instant expiresAt, boolean revoked) {
    return new RefreshToken(id, userId, tokenHash, expiresAt, revoked);
  }

  // ── Hashing ─────────────────────────────────────────────────────────────
  public static String hash(String rawToken) {
    try {
      var digest = MessageDigest.getInstance("SHA-256");
      var hashBytes = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));
      return HexFormat.of().formatHex(hashBytes);
    } catch (NoSuchAlgorithmException e) {
      throw new InvalidRefreshTokenException("Refresh token hashing is unavailable", e);
    }
  }

  // ── Behaviour ───────────────────────────────────────────────────────────
  public void validate() {
    if (revoked) {
      throw new InvalidRefreshTokenException("Refresh token has been revoked");
    }
    if (Instant.now().isAfter(expiresAt)) {
      throw new InvalidRefreshTokenException("Refresh token has expired");
    }
  }

  public void revoke() {
    this.revoked = true;
  }

  // ── Getters ───────────────────────────────────────────────────────────
  public UUID getId() {
    return id;
  }

  public UserId getUserId() {
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

  public record IssueResult(RefreshToken token, String rawToken) {}
}
