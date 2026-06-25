package dev.amir.synapse.identity.domain.exception;

import dev.amir.synapse.identity.domain.value_object.UserId;
import dev.amir.synapse.shared.domain.DomainException;

public final class UserNotFoundException extends DomainException {
  private static final long serialVersionUID = 1L;

  public UserNotFoundException(UserId userId) {
    super("User not found: " + userId);
  }

  @Override
  public String getErrorCode() {
    return "IDENTITY_USER_NOT_FOUND";
  }

  @Override
  public String getTitle() {
    return "User Not Found";
  }

  @Override
  public int getHttpStatus() {
    return 404;
  }
}
