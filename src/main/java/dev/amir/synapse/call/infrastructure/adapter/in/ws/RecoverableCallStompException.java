package dev.amir.synapse.call.infrastructure.adapter.in.ws;

import dev.amir.synapse.shared.domain.DomainException;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

class RecoverableCallStompException extends RuntimeException {
  private static final long serialVersionUID = 1L;
  private final DomainException domainException;
  private final UUID callId;

  RecoverableCallStompException(DomainException domainException, @Nullable UUID callId) {
    super(domainException);
    this.domainException = domainException;
    this.callId = callId;
  }

  DomainException domainException() {
    return domainException;
  }

  @Nullable UUID callId() {
    return callId;
  }
}
