package dev.amir.synapse.identity.application.exception;

import java.io.Serial;

/** Internal signal that an initial Handle candidate lost a database uniqueness race. */
public final class HandleConflictException extends RuntimeException {
  @Serial private static final long serialVersionUID = 1L;

  public HandleConflictException(Throwable cause) {
    super("Initial Handle candidate is already allocated", cause);
  }
}
