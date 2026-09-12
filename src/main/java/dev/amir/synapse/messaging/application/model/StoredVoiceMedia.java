package dev.amir.synapse.messaging.application.model;

import java.util.Arrays;
import java.util.Objects;

public record StoredVoiceMedia(String storageKey, String mimeType, long sizeBytes, byte[] sha256) {
  public StoredVoiceMedia {
    Objects.requireNonNull(storageKey, "Storage key cannot be null");
    Objects.requireNonNull(mimeType, "MIME type cannot be null");
    Objects.requireNonNull(sha256, "SHA-256 cannot be null");
    sha256 = Arrays.copyOf(sha256, sha256.length);
  }

  @Override
  public byte[] sha256() {
    return Arrays.copyOf(sha256, sha256.length);
  }
}
