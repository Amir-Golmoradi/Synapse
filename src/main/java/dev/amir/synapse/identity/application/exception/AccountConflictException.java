package dev.amir.synapse.identity.application.exception;

import dev.amir.synapse.shared.domain.DomainException;
import java.io.Serial;

/** A verified email belongs to a different external identity. */
public final class AccountConflictException extends DomainException {
  @Serial private static final long serialVersionUID = 1L;

  public AccountConflictException() {
    super("The verified email is already associated with another account.");
  }

  @Override
  public String getErrorCode() {
    return "IDENTITY_ACCOUNT_CONFLICT";
  }

  @Override
  public String getTitle() {
    return "Account Conflict";
  }

  @Override
  public int getHttpStatus() {
    return 409;
  }
}
