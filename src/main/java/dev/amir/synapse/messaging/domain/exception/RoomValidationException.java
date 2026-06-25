package dev.amir.synapse.messaging.domain.exception;

public class RoomValidationException extends RuntimeException {
  private static final long serialVersionUID = 1L;

  public RoomValidationException(String message) {
    super(message);
  }
}
