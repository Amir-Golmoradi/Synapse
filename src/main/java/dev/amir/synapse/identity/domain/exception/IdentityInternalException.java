package dev.amir.synapse.identity.domain.exception;

import dev.amir.synapse.shared.domain.DomainException;

public final class IdentityInternalException extends DomainException {
  private static final long serialVersionUID = 1L;

  public IdentityInternalException(String message, Throwable cause) {
    super(message, cause);
  }

  @Override
  public String getErrorCode() {
    return "IDENTITY_INTERNAL_ERROR";
  }

  @Override
  public String getTitle() {
    return "Identity Internal Error";
  }

  @Override
  public int getHttpStatus() {
    return 500;
  }
}
