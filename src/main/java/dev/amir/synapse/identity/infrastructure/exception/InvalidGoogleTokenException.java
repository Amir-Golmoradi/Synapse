package dev.amir.synapse.identity.infrastructure.exception;

import dev.amir.synapse.shared.domain.DomainException;

public final class InvalidGoogleTokenException extends DomainException {
  private static final long serialVersionUID = 1L;

  public InvalidGoogleTokenException(String message) {
    super(message);
  }

  public InvalidGoogleTokenException(String message, Throwable cause) {
    super(message, cause);
  }

  @Override
  public String getErrorCode() {
    return "IDENTITY_INVALID_GOOGLE_TOKEN";
  }

  @Override
  public String getTitle() {
    return "Invalid Google Token";
  }

  @Override
  public int getHttpStatus() {
    return 401;
  }
}
