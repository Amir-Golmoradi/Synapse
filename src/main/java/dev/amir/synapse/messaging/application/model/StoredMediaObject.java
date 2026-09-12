package dev.amir.synapse.messaging.application.model;

import java.time.Instant;
import java.util.Objects;

public record StoredMediaObject(String storageKey, Instant lastModified, boolean partial) {
  public StoredMediaObject {
    Objects.requireNonNull(storageKey, "Storage key cannot be null");
    Objects.requireNonNull(lastModified, "Last-modified timestamp cannot be null");
  }
}
