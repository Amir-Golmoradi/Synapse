package dev.amir.synapse.identity.domain.exception;

import dev.amir.synapse.shared.domain.DomainException;

public final class InvalidRefreshTokenException extends DomainException {
  private static final long serialVersionUID = 1L;

  public InvalidRefreshTokenException(String message) {
    super(message);
  }

  public InvalidRefreshTokenException(String message, Throwable cause) {
    super(message, cause);
  }

  @Override
  public String getErrorCode() {
    return "IDENTITY_INVALID_REFRESH_TOKEN";
  }

  @Override
  public String getTitle() {
    return "Invalid Refresh Token";
  }

  @Override
  public int getHttpStatus() {
    return 401;
  }
}
