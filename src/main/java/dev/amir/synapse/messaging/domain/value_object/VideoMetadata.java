package dev.amir.synapse.messaging.domain.value_object;

import dev.amir.synapse.messaging.domain.exception.MessageValidationException;
import java.util.Locale;
import org.jspecify.annotations.Nullable;

public record VideoMetadata(
    String contentType,
    long sizeBytes,
    long durationMs,
    int width,
    int height,
    VideoCodec videoCodec,
    @Nullable AudioCodec audioCodec) {

  public VideoMetadata {
    if (contentType == null) {
      throw invalid("Video content type is required.");
    }
    contentType = contentType.toLowerCase(Locale.ROOT);
    if (!"video/webm".equals(contentType) && !"video/mp4".equals(contentType)) {
      throw invalid("Video content type is unsupported.");
    }
    if (sizeBytes <= 0 || durationMs <= 0 || width <= 0 || height <= 0 || videoCodec == null) {
      throw invalid("Video metadata values must be positive and complete.");
    }
    var webm = "video/webm".equals(contentType);
    if (webm && videoCodec != VideoCodec.VP8 && videoCodec != VideoCodec.VP9) {
      throw invalid("WebM video codec is unsupported.");
    }
    if (!webm && videoCodec != VideoCodec.H264) {
      throw invalid("MP4 video codec is unsupported.");
    }
    if (webm && audioCodec != null && audioCodec != AudioCodec.OPUS) {
      throw invalid("WebM audio codec is unsupported.");
    }
    if (!webm && audioCodec != null && audioCodec != AudioCodec.AAC) {
      throw invalid("MP4 audio codec is unsupported.");
    }
  }

  private static MessageValidationException invalid(String message) {
    return new MessageValidationException(message);
  }

  public enum VideoCodec {
    VP8,
    VP9,
    H264
  }

  public enum AudioCodec {
    OPUS,
    AAC
  }
}
