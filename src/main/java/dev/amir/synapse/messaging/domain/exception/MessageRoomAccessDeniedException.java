package dev.amir.synapse.messaging.domain.exception;

import dev.amir.synapse.shared.domain.DomainException;

public final class MessageRoomAccessDeniedException extends DomainException {
  private static final long serialVersionUID = 1L;

  public MessageRoomAccessDeniedException() {
    super("The room was not found or is not accessible.");
  }

  @Override
  public String getErrorCode() {
    return "MESSAGE_ROOM_NOT_FOUND";
  }

  @Override
  public String getTitle() {
    return "Message room not found";
  }

  @Override
  public int getHttpStatus() {
    return 404;
  }
}
