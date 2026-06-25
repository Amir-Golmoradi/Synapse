package dev.amir.synapse.identity.domain.value_object;

import dev.amir.synapse.shared.domain.ValueObject;
import java.util.List;

public final class FullName extends ValueObject {
  private final String firstName;
  private final String lastName;

  private FullName(String firstName, String lastName) {
    this.firstName = validateName(firstName);
    this.lastName = validateName(lastName);
  }

  public static FullName of(String firstName, String lastName) {
    return new FullName(firstName, lastName);
  }

  private static String validateName(String value) {
    if (value == null) {
      return "";
    }

    return value.trim();
  }

  @Override
  public List<Object> getAtomicValues() {
    return List.of(firstName, lastName);
  }

  public String getFirstName() {
    return firstName;
  }

  public String getLastName() {
    return lastName;
  }
}
