package dev.amir.synapse.identity.domain.value_object;

import dev.amir.synapse.identity.domain.exception.InvalidUserIdentifierException;
import dev.amir.synapse.shared.domain.ValueObject;
import java.util.Objects;
import java.util.UUID;

public record UserId(UUID value) implements ValueObject {
  public UserId {
    value = Objects.requireNonNull(value, "UserId value cannot be null");
  }

  public static UserId generate() {
    return new UserId(UUID.randomUUID());
  }

  public static UserId of(String value) {
    return fromString(value);
  }

  public static UserId fromString(String uuidString) {
    try {
      return new UserId(UUID.fromString(uuidString));
    } catch (IllegalArgumentException e) {
      throw new InvalidUserIdentifierException(e);
    }
  }

  public UUID getValue() {
    return value;
  }
}
