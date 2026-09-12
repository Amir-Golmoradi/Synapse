package dev.amir.synapse.messaging.domain.port.out;

import java.util.Arrays;
import java.util.Objects;
import java.util.UUID;

public record VoiceMessageWriteRequest(
    UUID messageId,
    UUID roomId,
    UUID senderId,
    UUID clientMessageId,
    String storageKey,
    String mimeType,
    long sizeBytes,
    int durationMs,
    byte[] sha256) {
  public VoiceMessageWriteRequest {
    Objects.requireNonNull(messageId, "Message ID cannot be null");
    Objects.requireNonNull(roomId, "Room ID cannot be null");
    Objects.requireNonNull(senderId, "Sender ID cannot be null");
    Objects.requireNonNull(clientMessageId, "Client message ID cannot be null");
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
