package dev.amir.synapse.messaging.domain.exception;

import dev.amir.synapse.shared.domain.DomainException;

public final class VoiceMediaTooLargeException extends DomainException {
  private static final long serialVersionUID = 1L;

  public VoiceMediaTooLargeException() {
    super("The voice message exceeds the maximum allowed size.");
  }

  @Override
  public String getErrorCode() {
    return "VOICE_MESSAGE_TOO_LARGE";
  }

  @Override
  public String getTitle() {
    return "Voice message is too large";
  }

  @Override
  public int getHttpStatus() {
    return 413;
  }
}
