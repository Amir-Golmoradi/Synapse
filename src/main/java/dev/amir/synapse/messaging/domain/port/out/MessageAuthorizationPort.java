package dev.amir.synapse.messaging.domain.port.out;

import java.util.UUID;

@FunctionalInterface
public interface MessageAuthorizationPort {
  boolean canSend(UUID roomId, UUID senderId);
}
