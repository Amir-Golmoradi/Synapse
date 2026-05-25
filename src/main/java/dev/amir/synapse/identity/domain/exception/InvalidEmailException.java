package dev.amir.synapse.identity.domain.exception;

import dev.amir.synapse.shared.domain.DomainException;

public final class InvalidEmailException extends DomainException {
  private static final long serialVersionUID = 1L;
  private static final String MESSAGE = "Email cannot be empty or null";

  public InvalidEmailException() {
    super(MESSAGE);
  }

  @Override
  public String getErrorCode() {
    return "IDENTITY_INVALID_EMAIL";
  }

  @Override
  public String getTitle() {
    return "Invalid Email";
  }

  @Override
  public int getHttpStatus() {
    return 400;
  }
}
