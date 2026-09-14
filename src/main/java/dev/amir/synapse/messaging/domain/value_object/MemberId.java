package dev.amir.synapse.messaging.domain.value_object;

import dev.amir.synapse.shared.domain.ValueObject;
import java.util.Objects;
import java.util.UUID;

public record MemberId(UUID value) implements ValueObject {
  public MemberId {
    Objects.requireNonNull(value, "MemberId cannot be null");
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
}
