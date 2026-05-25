package dev.amir.synapse.identity.domain.exception;

import dev.amir.synapse.shared.domain.DomainException;

public final class InvalidEmailFormatException extends DomainException {
  private static final long serialVersionUID = 1L;
  private static final String MESSAGE = "Email format is not valid";

  public InvalidEmailFormatException() {
    super(MESSAGE);
  }

  @Override
  public String getErrorCode() {
    return "IDENTITY_INVALID_EMAIL_FORMAT";
  }

  @Override
  public String getTitle() {
    return "Invalid Email Format";
  }

  @Override
  public int getHttpStatus() {
    return 400;
  }
}
