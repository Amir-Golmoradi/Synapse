package dev.amir.synapse.messaging.domain.exception;

import dev.amir.synapse.shared.domain.DomainException;

public class RoomValidationException extends DomainException {
  private static final long serialVersionUID = 1L;

  public RoomValidationException(String message) {
    super(message);
  }

  @Override
  public String getErrorCode() {
    return "ROOM_VALIDATION_FAILED";
  }

  @Override
  public String getTitle() {
    return "Room validation failed";
  }

  @Override
  public int getHttpStatus() {
    return 400;
  }
}
