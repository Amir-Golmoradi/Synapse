package dev.amir.synapse.messaging.domain.port.out;

import java.util.UUID;

@FunctionalInterface
public interface MessageSendAuthorizationPort {
  boolean canSend(UUID roomId, UUID senderId);
}
