package dev.amir.synapse.messaging.domain.value_object;

import dev.amir.synapse.shared.domain.ValueObject;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public final class RoomId extends ValueObject {
  private final UUID value;

  private RoomId(UUID value) {
    this.value = Objects.requireNonNull(value, "RoomId cannot be null");
  }

  public static RoomId generate() {
    return new RoomId(UUID.randomUUID());
  }

  public static RoomId of(UUID value) {
    return new RoomId(value);
  }

  public static RoomId fromString(String value) {
    if (!value.isBlank()) {
      return new RoomId(UUID.fromString(value));
    }
    throw new IllegalArgumentException("RoomId cannot be empty");
  }

  @Override
  public List<Object> getAtomicValues() {
    return List.of(value);
  }

  public UUID getValue() {
    return value;
  }

  @Override
  public boolean equals(Object o) {
    if (o == null) return false;
    if (getClass() != o.getClass()) return false;
    if (!super.equals(o)) return false;

    RoomId roomId = (RoomId) o;
    return getValue().equals(roomId.getValue());
  }

  @Override
  public int hashCode() {
    return Objects.hash(value);
  }
}
