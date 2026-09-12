package dev.amir.synapse.messaging.domain.exception;

import dev.amir.synapse.shared.domain.DomainException;

public final class InvalidVoiceMediaException extends DomainException {
  private static final long serialVersionUID = 1L;

  public InvalidVoiceMediaException(String message) {
    super(message);
  }

  public InvalidVoiceMediaException(String message, Throwable cause) {
    super(message, cause);
  }

  @Override
  public String getErrorCode() {
    return "VOICE_MESSAGE_INVALID_MEDIA";
  }

  @Override
  public String getTitle() {
    return "Voice message media is invalid";
  }

  @Override
  public int getHttpStatus() {
    return 400;
  }
}
