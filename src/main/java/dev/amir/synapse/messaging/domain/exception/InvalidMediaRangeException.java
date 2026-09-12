package dev.amir.synapse.messaging.domain.exception;

import dev.amir.synapse.shared.domain.DomainException;

public final class InvalidMediaRangeException extends DomainException {
  private static final long serialVersionUID = 1L;

  public InvalidMediaRangeException() {
    super("The requested media byte range is invalid or unsatisfiable.");
  }

  public InvalidMediaRangeException(Throwable cause) {
    super("The requested media byte range is invalid or unsatisfiable.", cause);
  }

  @Override
  public String getErrorCode() {
    return "MESSAGE_MEDIA_RANGE_INVALID";
  }

  @Override
  public String getTitle() {
    return "Message media range is invalid";
  }

  @Override
  public int getHttpStatus() {
    return 416;
  }
}
