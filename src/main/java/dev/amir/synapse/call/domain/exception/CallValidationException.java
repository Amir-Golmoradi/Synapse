package dev.amir.synapse.call.domain.exception;

import dev.amir.synapse.shared.domain.DomainException;

public class CallValidationException extends DomainException {
  private static final long serialVersionUID = 1L;

  public CallValidationException(String message) {
    super(message);
  }

  @Override
  public String getErrorCode() {
    return "CALL_VALIDATION_FAILED";
  }

  @Override
  public String getTitle() {
    return "Call request is invalid";
  }

  @Override
  public int getHttpStatus() {
    return 400;
  }
}
