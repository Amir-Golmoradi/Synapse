package dev.amir.synapse.messaging.domain.exception;

import dev.amir.synapse.shared.domain.DomainException;

public final class UnsupportedVoiceMediaException extends DomainException {
  private static final long serialVersionUID = 1L;

  public UnsupportedVoiceMediaException() {
    super("The voice message media type is not supported.");
  }

  @Override
  public String getErrorCode() {
    return "VOICE_MESSAGE_UNSUPPORTED_MEDIA";
  }

  @Override
  public String getTitle() {
    return "Voice message media type is unsupported";
  }

  @Override
  public int getHttpStatus() {
    return 415;
  }
}
