package dev.amir.synapse.identity.domain.value_object;

import dev.amir.synapse.identity.domain.exception.InvalidUserIdentifierException;
import dev.amir.synapse.shared.domain.ValueObject;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public final class UserId extends ValueObject {

  private final UUID value;

  public UserId(UUID value) {
    this.value = Objects.requireNonNull(value, "UserId value cannot be null");
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

  @Override
  public boolean equals(Object o) {
    if (!(o instanceof UserId that)) return false;
    if (!super.equals(o)) return false;
    return Objects.equals(value, that.value);
  }

  @Override
  public int hashCode() {
    return Objects.hash(super.hashCode(), value);
  }

  @Override
  public List<Object> getAtomicValues() {
    return List.of(value);
  }
}
