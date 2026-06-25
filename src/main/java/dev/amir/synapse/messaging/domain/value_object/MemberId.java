package dev.amir.synapse.messaging.domain.value_object;

import dev.amir.synapse.shared.domain.ValueObject;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class MemberId extends ValueObject {
  private final UUID value;

  public MemberId(UUID value) {
    this.value = Objects.requireNonNull(value, "MemberId cannot be null");
  }

  public static MemberId of(UUID value) {
    return new MemberId(value);
  }

  public static MemberId generate() {
    return MemberId.of(UUID.randomUUID());
  }

  public static MemberId fromString(String value) {
    return MemberId.of(UUID.fromString(value));
  }

  public UUID getValue() {
    return value;
  }

  @Override
  public List<Object> getAtomicValues() {
    return List.of();
  }

  @Override
  public boolean equals(Object o) {
    if (o == null || getClass() != o.getClass()) return false;
    if (!super.equals(o)) return false;

    MemberId memberId = (MemberId) o;
    return getValue().equals(memberId.getValue());
  }

  @Override
  public int hashCode() {
    return Objects.hash(super.hashCode(), getValue());
  }
}
