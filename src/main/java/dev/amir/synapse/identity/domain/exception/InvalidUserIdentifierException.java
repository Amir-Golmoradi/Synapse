package dev.amir.synapse.identity.domain.exception;

import dev.amir.synapse.shared.domain.DomainException;
import java.io.Serial;

public final class InvalidUserIdentifierException extends DomainException {
  @Serial private static final long serialVersionUID = 1L;
  private static final String MESSAGE =
      "User identifier must be a valid UUID string, for example 123e4567-e89b-12d3-a456-426614174000.";

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
