package dev.amir.synapse.identity.domain.event;

import dev.amir.synapse.identity.domain.value_object.Email;
import dev.amir.synapse.identity.domain.value_object.UserId;
import dev.amir.synapse.shared.domain.DomainEvent;
import java.time.Instant;

public record UserRegisteredEvent(UserId id, Email email, Instant occurredOn)
    implements DomainEvent {
  public UserRegisteredEvent(UserId id, Email email) {
    this(id, email, Instant.now());
  }

  @Override
  public Instant occurredOn() {
    return occurredOn;
  }
}
