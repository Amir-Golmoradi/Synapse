package dev.amir.synapse.identity.infrastructure.adapter.out.google;

import dev.amir.synapse.identity.infrastructure.exception.InvalidGoogleTokenException;
import java.time.Clock;
import java.time.Instant;
import java.util.Set;
import org.jspecify.annotations.Nullable;
import org.springframework.util.StringUtils;

final class GoogleTokenValidator {

  private static final Set<String> VALID_ISSUERS =
      Set.of("accounts.google.com", "https://accounts.google.com");

  private final String googleClientId;
  private final Clock clock;

  GoogleTokenValidator(String googleClientId) {
    this(googleClientId, Clock.systemUTC());
  }

  GoogleTokenValidator(String googleClientId, Clock clock) {
    this.googleClientId = googleClientId;
    this.clock = clock;
  }

  void validate(@Nullable TokenInfoResponse response) {
    if (response == null) {
      throw new InvalidGoogleTokenException("Google token response is empty");
    }

    var audience = response.aud();
    if (!StringUtils.hasText(audience)) {
      throw new InvalidGoogleTokenException("Google token audience is empty");
    }

    if (!googleClientId.equals(audience)) {
      throw new InvalidGoogleTokenException("Google token audience mismatch");
    }

    var issuer = response.iss();
    if (issuer == null || !VALID_ISSUERS.contains(issuer)) {
      throw new InvalidGoogleTokenException("Google token issuer is invalid");
    }

    var subject = response.sub();
    if (!StringUtils.hasText(subject)) {
      throw new InvalidGoogleTokenException("Google token subject is missing");
    }

    var email = response.email();
    if (!StringUtils.hasText(email)) {
      throw new InvalidGoogleTokenException("Google token email is missing");
    }

    var emailVerified = response.emailVerified();
    if (!Boolean.TRUE.equals(emailVerified)) {
      throw new InvalidGoogleTokenException("Google token email verification failed");
    }

    var expiration = response.exp();
    if (expiration == null) {
      throw new InvalidGoogleTokenException("Google token has expired");
    }

    var expiresAt = Instant.ofEpochSecond(expiration);
    var now = Instant.now(clock);

    if (!expiresAt.isAfter(now)) {
      throw new InvalidGoogleTokenException("Google token has expired");
    }
  }
}
