package dev.amir.synapse.messaging.domain.port.out;

import java.util.Arrays;
import java.util.Optional;
import java.util.UUID;

public interface MessageMediaReadPort {
  Optional<MessageMediaDescriptor> findAuthorized(UUID messageId, UUID requesterId);

  boolean isStorageKeyReferenced(String storageKey);

  record MessageMediaDescriptor(
      UUID messageId, String storageKey, String contentType, long sizeBytes, byte[] sha256) {
    public MessageMediaDescriptor {
      sha256 = Arrays.copyOf(sha256, sha256.length);
    }

    @Override
    public byte[] sha256() {
      return Arrays.copyOf(sha256, sha256.length);
    }
  }
}
