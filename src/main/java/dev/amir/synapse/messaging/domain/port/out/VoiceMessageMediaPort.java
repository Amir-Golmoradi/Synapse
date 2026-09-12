package dev.amir.synapse.messaging.domain.port.out;

import java.util.Optional;
import java.util.UUID;

public interface VoiceMessageMediaPort {
  Optional<VoiceMediaRecord> findAuthorized(UUID roomId, UUID messageId, UUID requesterId);

  boolean isStorageKeyReferenced(String storageKey);
}
