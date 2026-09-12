package dev.amir.synapse.messaging.domain.port.in.get_voice_media;

import java.io.InputStream;
import java.util.Objects;

public record VoiceMediaDownload(
    InputStream content,
    String mimeType,
    long totalSize,
    long offset,
    long contentLength,
    boolean partial) {
  public VoiceMediaDownload {
    Objects.requireNonNull(content, "Media content cannot be null");
    Objects.requireNonNull(mimeType, "MIME type cannot be null");
    if (totalSize <= 0 || offset < 0 || contentLength <= 0 || offset + contentLength > totalSize) {
      throw new IllegalArgumentException("Invalid media download bounds");
    }
  }
}
