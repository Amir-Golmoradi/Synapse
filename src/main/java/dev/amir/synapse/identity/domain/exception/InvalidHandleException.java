package dev.amir.synapse.identity.domain.exception;

import dev.amir.synapse.shared.domain.DomainException;
import java.io.Serial;

public final class InvalidHandleException extends DomainException {
  @Serial private static final long serialVersionUID = 1L;

  private InvalidHandleException(String message) {
    super(message);
  }

  public static InvalidHandleException missing() {
    return new InvalidHandleException("Handle is required and cannot be null.");
  }

  public static InvalidHandleException unsupportedFormat() {
    return new InvalidHandleException(
        "Handle must be 2 to 32 characters using only lowercase letters, digits, periods, and underscores, without consecutive periods.");
  }

  public static InvalidHandleException reserved(String value) {
    return new InvalidHandleException(
        "Handle '" + value + "' is reserved and cannot be registered.");
  }

  @Override
  public String getErrorCode() {
    return "IDENTITY_INVALID_HANDLE";
  }

  @Override
  public String getTitle() {
    return "Invalid Handle";
  }

  @Override
  public int getHttpStatus() {
    return 400;
  }
}
