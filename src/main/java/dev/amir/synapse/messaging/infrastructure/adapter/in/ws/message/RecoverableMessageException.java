package dev.amir.synapse.messaging.infrastructure.adapter.in.ws.message;

import dev.amir.synapse.shared.domain.DomainException;
import java.io.Serial;
import java.util.Objects;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

final class RecoverableMessageException extends RuntimeException {
  @Serial private static final long serialVersionUID = 1L;

  private final DomainException domainException;
  private final UUID roomId;
  private final @Nullable UUID clientMessageId;

  RecoverableMessageException(
      DomainException domainException, UUID roomId, @Nullable UUID clientMessageId) {
    super("A recoverable message send error occurred", domainException);
    this.domainException =
        Objects.requireNonNull(domainException, "Domain exception cannot be null");
    this.roomId = Objects.requireNonNull(roomId, "Room ID cannot be null");
    this.clientMessageId = clientMessageId;
  }

  DomainException getDomainException() {
    return domainException;
  }

  UUID getRoomId() {
    return roomId;
  }

  @Nullable UUID getClientMessageId() {
    return clientMessageId;
  }
}
