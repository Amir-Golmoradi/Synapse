package dev.amir.synapse.identity.domain.value_object;

import dev.amir.synapse.identity.domain.exception.InvalidEmailException;
import dev.amir.synapse.identity.domain.exception.InvalidEmailFormatException;
import dev.amir.synapse.shared.domain.ValueObject;
import java.util.regex.Pattern;

public record Email(String value) implements ValueObject {
  private static final Pattern EMAIL_REGEX =
      Pattern.compile("^[a-zA-Z0-9._%+\\-]+@[a-zA-Z0-9.\\-]+\\.[a-zA-Z]{2,}$");

  public Email {
    validateEmail(value);
    value = value.trim();
  }

  public static Email of(String value) {
    return new Email(value);
  }

  private static void validateEmail(String email) {
    if (email == null || email.trim().isEmpty()) {
      throw new InvalidEmailException();
    }
    if (!EMAIL_REGEX.matcher(email).matches()) {
      throw new InvalidEmailFormatException();
    }
  }

  public String getValue() {
    return value;
  }
}
