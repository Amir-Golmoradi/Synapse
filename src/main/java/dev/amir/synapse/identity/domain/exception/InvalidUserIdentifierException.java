package dev.amir.synapse.identity.domain.exception;

import dev.amir.synapse.shared.domain.DomainException;

public final class InvalidUserIdentifierException extends DomainException {
  private static final long serialVersionUID = 1L;
  private static final String MESSAGE = "User identifier format is not correct";

  public InvalidUserIdentifierException() {
    super(MESSAGE);
  }

  public InvalidUserIdentifierException(Throwable cause) {
    super(MESSAGE, cause);
  }

  @Override
  public String getErrorCode() {
    return "IDENTITY_INVALID_USER_IDENTIFIER";
  }

  @Override
  public String getTitle() {
    return "Invalid User Identifier";
  }

  @Override
  public int getHttpStatus() {
    return 400;
  }
}
