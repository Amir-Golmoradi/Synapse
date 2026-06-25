package dev.amir.synapse.identity.domain.exception;

import dev.amir.synapse.shared.domain.DomainException;

public final class InvalidIdentityRequestException extends DomainException {
  private static final long serialVersionUID = 1L;

  public InvalidIdentityRequestException(String message, Throwable cause) {
    super(message, cause);
  }

  @Override
  public String getErrorCode() {
    return "IDENTITY_INVALID_REQUEST";
  }

  @Override
  public String getTitle() {
    return "Invalid Identity Request";
  }

  @Override
  public int getHttpStatus() {
    return 400;
  }
}
