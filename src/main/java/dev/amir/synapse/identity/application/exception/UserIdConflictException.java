package dev.amir.synapse.identity.application.exception;

import java.io.Serial;

/** Internal signal for an impossible generated UserId collision. */
public final class UserIdConflictException extends RuntimeException {
  @Serial private static final long serialVersionUID = 1L;

  public UserIdConflictException(Throwable cause) {
    super("Generated user identifier is already allocated", cause);
  }
}
