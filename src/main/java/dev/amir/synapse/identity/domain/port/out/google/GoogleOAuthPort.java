package dev.amir.synapse.identity.domain.port.out.google;

import dev.amir.synapse.identity.domain.model.GoogleUserInfo;

@FunctionalInterface
public interface GoogleOAuthPort {
  // Verifies the ID token with Google.
  // Throws InvalidGoogleTokenException if invalid or expired.
  GoogleUserInfo verifyIdToken(String idToken);
}
