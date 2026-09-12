package dev.amir.synapse.messaging.domain.exception;

import dev.amir.synapse.shared.domain.DomainException;

public final class VoiceMediaStorageException extends DomainException {
  private static final long serialVersionUID = 1L;

  public VoiceMediaStorageException(String message, Throwable cause) {
    super(message, cause);
  }

  @Override
  public String getErrorCode() {
    return "VOICE_MEDIA_UNAVAILABLE";
  }

  @Override
  public String getTitle() {
    return "Voice media is unavailable";
  }

  @Override
  public int getHttpStatus() {
    return 503;
  }
}
