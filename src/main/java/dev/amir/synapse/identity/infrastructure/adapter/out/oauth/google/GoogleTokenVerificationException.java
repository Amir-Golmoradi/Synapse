package dev.amir.synapse.identity.infrastructure.adapter.out.oauth.google;

import java.io.Serial;

final class GoogleTokenVerificationException extends RuntimeException {
  @Serial private static final long serialVersionUID = 1L;

  GoogleTokenVerificationException(String message) {
    super(message);
  }
}
