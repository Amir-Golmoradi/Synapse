package dev.amir.synapse.messaging.domain.exception;

import dev.amir.synapse.shared.domain.DomainException;

public final class MessageIdempotencyConflictException extends DomainException {
  private static final long serialVersionUID = 1L;

  public MessageIdempotencyConflictException() {
    super("The client message ID has already been used for different message data.");
  }

  @Override
  public String getErrorCode() {
    return "MESSAGE_IDEMPOTENCY_CONFLICT";
  }

  @Override
  public String getTitle() {
    return "Message idempotency conflict";
  }

  @Override
  public int getHttpStatus() {
    return 409;
  }
}
