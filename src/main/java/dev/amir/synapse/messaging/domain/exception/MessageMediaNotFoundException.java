package dev.amir.synapse.messaging.domain.exception;

import dev.amir.synapse.shared.domain.DomainException;

public final class MessageMediaNotFoundException extends DomainException {
  private static final long serialVersionUID = 1L;

  public MessageMediaNotFoundException() {
    super("The message media was not found or is not accessible.");
  }

  @Override
  public String getErrorCode() {
    return "MESSAGE_MEDIA_NOT_FOUND";
  }

  @Override
  public String getTitle() {
    return "Message media not found";
  }

  @Override
  public int getHttpStatus() {
    return 404;
  }
}
