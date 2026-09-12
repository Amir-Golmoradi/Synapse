package dev.amir.synapse.messaging.domain.value_object;

import dev.amir.synapse.messaging.domain.exception.MessageValidationException;
import dev.amir.synapse.shared.domain.ValueObject;
import java.util.Locale;
import java.util.Set;

public record VoiceMessageMetadata(int durationMs, String mimeType, long sizeBytes)
    implements ValueObject {
  public static final int MAX_DURATION_MS = 300_000;
  public static final long MAX_SIZE_BYTES = 16L * 1024 * 1024;
  public static final Set<String> SUPPORTED_MIME_TYPES =
      Set.of("audio/webm", "audio/ogg", "audio/mp4");

  public VoiceMessageMetadata {
    mimeType = normalizeMimeType(mimeType);
    if (durationMs <= 0 || durationMs > MAX_DURATION_MS) {
      throw new MessageValidationException(
          "Voice message duration must be between 1 and " + MAX_DURATION_MS + " milliseconds.");
    }
    if (sizeBytes <= 0 || sizeBytes > MAX_SIZE_BYTES) {
      throw new MessageValidationException(
          "Voice message size must be between 1 and " + MAX_SIZE_BYTES + " bytes.");
    }
    if (!SUPPORTED_MIME_TYPES.contains(mimeType)) {
      throw new MessageValidationException("Voice message MIME type is not supported.");
    }
  }

  public static String normalizeMimeType(String mimeType) {
    if (mimeType == null || mimeType.isBlank()) {
      throw new MessageValidationException("Voice message MIME type is required.");
    }
    var separator = mimeType.indexOf(';');
    var baseType = separator < 0 ? mimeType : mimeType.substring(0, separator);
    return baseType.strip().toLowerCase(Locale.ROOT);
  }
}
