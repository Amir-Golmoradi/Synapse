package dev.amir.synapse.identity.application.exception;

import java.io.Serial;

/** Internal signal that another account already owns the verified email. */
public final class EmailConflictException extends RuntimeException {
  @Serial private static final long serialVersionUID = 1L;

  public EmailConflictException(Throwable cause) {
    super("Verified email is already registered", cause);
  }
}
