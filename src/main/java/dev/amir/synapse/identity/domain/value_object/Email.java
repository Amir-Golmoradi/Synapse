package dev.amir.synapse.identity.domain.value_object;

import dev.amir.synapse.identity.domain.exception.InvalidEmailException;
import dev.amir.synapse.identity.domain.exception.InvalidEmailFormatException;
import dev.amir.synapse.shared.domain.ValueObject;
import java.util.List;
import java.util.regex.Pattern;

public final class Email extends ValueObject {
  private static final Pattern EMAIL_REGEX =
      Pattern.compile("^[a-zA-Z0-9._%+\\-]+@[a-zA-Z0-9.\\-]+\\.[a-zA-Z]{2,}$");

  private final String value;

  private Email(String value) {
    validateEmail(value);
    this.value = value.trim();
  }

  public static Email of(String value) {
    return new Email(value);
  }

  private void validateEmail(String email) {
    if (email.isEmpty()) {
      throw new InvalidEmailException();
    }
    if (!EMAIL_REGEX.matcher(email).matches()) {
      throw new InvalidEmailFormatException();
    }
  }

  @Override
  public List<Object> getAtomicValues() {
    return List.of(value);
  }

  public String getValue() {
    return value;
  }
}
