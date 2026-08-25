package dev.amir.synapse.messaging.domain.exception;

import dev.amir.synapse.shared.domain.DomainException;

public final class MessageValidationException extends DomainException {
  private static final long serialVersionUID = 1L;

  public MessageValidationException(String message) {
    super(message);
  }

  @Override
  public String getErrorCode() {
    return "MESSAGE_VALIDATION_FAILED";
  }

  @Override
  public String getTitle() {
    return "Message validation failed";
  }

  @Override
  public int getHttpStatus() {
    return 400;
  }
}
