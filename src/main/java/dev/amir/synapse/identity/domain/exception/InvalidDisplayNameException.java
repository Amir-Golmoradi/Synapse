package dev.amir.synapse.identity.domain.exception;

import dev.amir.synapse.shared.domain.DomainException;
import java.io.Serial;

public final class InvalidDisplayNameException extends DomainException {
  @Serial private static final long serialVersionUID = 1L;

  private InvalidDisplayNameException(String message) {
    super(message);
  }

  public static InvalidDisplayNameException missing() {
    return new InvalidDisplayNameException("Display name is required and cannot be null.");
  }

  public static InvalidDisplayNameException blank() {
    return new InvalidDisplayNameException(
        "Display name must contain visible characters after trimming whitespace.");
  }

  public static InvalidDisplayNameException unsupportedFormat() {
    return new InvalidDisplayNameException(
        "Display name must contain between 1 and 32 visible Unicode characters.");
  }

  public static InvalidDisplayNameException containsInvisibleCharacters() {
    return new InvalidDisplayNameException(
        "Display name cannot contain control or invisible formatting characters.");
  }

  @Override
  public String getErrorCode() {
    return "IDENTITY_INVALID_DISPLAY_NAME";
  }

  @Override
  public String getTitle() {
    return "Invalid Display Name";
  }

  @Override
  public int getHttpStatus() {
    return 400;
  }
}
