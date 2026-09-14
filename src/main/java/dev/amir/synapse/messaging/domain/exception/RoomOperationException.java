package dev.amir.synapse.messaging.domain.exception;

import dev.amir.synapse.shared.domain.DomainException;
import java.util.Objects;

public abstract class RoomOperationException extends DomainException {
  private static final long serialVersionUID = 1L;

  private final String errorCode;
  private final String title;
  private final int httpStatus;

  protected RoomOperationException(String message, String errorCode, String title, int httpStatus) {
    super(message);
    this.errorCode = Objects.requireNonNull(errorCode, "Error code cannot be null");
    this.title = Objects.requireNonNull(title, "Error title cannot be null");
    this.httpStatus = httpStatus;
  }

  @Override
  public String getErrorCode() {
    return errorCode;
  }

  @Override
  public String getTitle() {
    return title;
  }

  @Override
  public int getHttpStatus() {
    return httpStatus;
  }
}
