package dev.amir.synapse.call.domain.value_object;

import dev.amir.synapse.call.domain.exception.CallValidationException;
import dev.amir.synapse.shared.domain.ValueObject;
import java.util.Objects;
import java.util.UUID;

public record CallParticipants(UUID callerId, UUID calleeId) implements ValueObject {
  public CallParticipants {
    Objects.requireNonNull(callerId, "Caller ID cannot be null");
    Objects.requireNonNull(calleeId, "Callee ID cannot be null");
    if (callerId.equals(calleeId)) {
      throw new CallValidationException("A voice call requires two different users.");
    }
  }

  public boolean contains(UUID userId) {
    return callerId.equals(userId) || calleeId.equals(userId);
  }

  public UUID counterpartOf(UUID userId) {
    if (callerId.equals(userId)) {
      return calleeId;
    }
    if (calleeId.equals(userId)) {
      return callerId;
    }
    throw new CallValidationException("The user is not a call participant.");
  }
}
