package dev.amir.synapse.messaging.domain.value_object;

import dev.amir.synapse.shared.domain.ValueObject;
import java.util.Objects;
import java.util.UUID;

public record RoomId(UUID value) implements ValueObject {
  public RoomId {
    Objects.requireNonNull(value, "RoomId cannot be null");
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

  public UUID getValue() {
    return value;
  }
}
