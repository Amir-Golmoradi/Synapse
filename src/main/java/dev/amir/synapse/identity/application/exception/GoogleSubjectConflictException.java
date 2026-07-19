package dev.amir.synapse.identity.application.exception;

import java.io.Serial;

/** Internal signal that another request created the same Google subject first. */
public final class GoogleSubjectConflictException extends RuntimeException {
  @Serial private static final long serialVersionUID = 1L;

  public GoogleSubjectConflictException(Throwable cause) {
    super("Google subject is already registered", cause);
  }
}
