package dev.amir.synapse.identity.infrastructure.exception;

import dev.amir.synapse.shared.domain.DomainException;

public final class InvalidTokenException extends DomainException {
  private static final long serialVersionUID = 1L;

  public InvalidTokenException(String message) {
    super(message);
  }

  public InvalidTokenException(String message, Throwable ex) {
    super(message, ex);
  }

  @Override
  public String getErrorCode() {
    return "IDENTITY_INVALID_TOKEN";
  }

  @Override
  public String getTitle() {
    return "Invalid Token";
  }

  @Override
  public int getHttpStatus() {
    return 401;
  }
}
