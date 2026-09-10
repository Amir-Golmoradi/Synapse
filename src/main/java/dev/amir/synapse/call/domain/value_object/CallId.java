package dev.amir.synapse.call.domain.value_object;

import dev.amir.synapse.shared.domain.ValueObject;
import java.util.Objects;
import java.util.UUID;

public record CallId(UUID value) implements ValueObject {
  public CallId {
    Objects.requireNonNull(value, "Call ID cannot be null");
  }

  public static CallId generate() {
    return new CallId(UUID.randomUUID());
  }

  public static CallId of(UUID value) {
    return new CallId(value);
  }
}
