package dev.amir.synapse.identity.domain.exception;

import dev.amir.synapse.shared.domain.DomainException;

public final class InvalidFullNameException extends DomainException {
  private static final long serialVersionUID = 1L;

  public InvalidFullNameException(String message) {
    super(message);
  }

  @Override
  public String getErrorCode() {
    return "IDENTITY_INVALID_FULL_NAME";
  }

  @Override
  public String getTitle() {
    return "Invalid Full Name";
  }

  @Override
  public int getHttpStatus() {
    return 400;
  }
}
