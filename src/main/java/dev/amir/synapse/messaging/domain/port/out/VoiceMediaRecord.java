package dev.amir.synapse.messaging.domain.port.out;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record VoiceMediaRecord(
    UUID messageId,
    UUID roomId,
    String storageKey,
    String mimeType,
    long sizeBytes,
    int durationMs,
    Instant createdAt) {
  public VoiceMediaRecord {
    Objects.requireNonNull(messageId, "Message ID cannot be null");
    Objects.requireNonNull(roomId, "Room ID cannot be null");
    Objects.requireNonNull(storageKey, "Storage key cannot be null");
    Objects.requireNonNull(mimeType, "MIME type cannot be null");
    Objects.requireNonNull(createdAt, "Creation timestamp cannot be null");
  }
}
