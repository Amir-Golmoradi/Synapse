package dev.amir.synapse.identity.application.port.out.google;

import dev.amir.synapse.identity.domain.value_object.GoogleUserInfo;

@FunctionalInterface
public interface GoogleOAuthPort {
  // Verifies the ID token with Google.
  // Throws InvalidGoogleTokenException if invalid or expired.
  GoogleUserInfo verifyIdToken(String idToken);
}
