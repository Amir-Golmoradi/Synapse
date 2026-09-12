package dev.amir.synapse.messaging.infrastructure.adapter.out.storage;

import dev.amir.synapse.messaging.domain.exception.InvalidVoiceMediaException;
import dev.amir.synapse.messaging.domain.exception.UnsupportedVoiceMediaException;
import dev.amir.synapse.messaging.domain.value_object.VoiceMessageMetadata;
import java.nio.charset.StandardCharsets;

final class VoiceMediaHeaderInspector {
  private static final int MINIMUM_HEADER_LENGTH = 12;
  private static final byte[] EBML = {(byte) 0x1A, (byte) 0x45, (byte) 0xDF, (byte) 0xA3};

  String inspect(String declaredMimeType, byte[] prefix) {
    var normalized = VoiceMessageMetadata.normalizeMimeType(declaredMimeType);
    if (!VoiceMessageMetadata.SUPPORTED_MIME_TYPES.contains(normalized)) {
      throw new UnsupportedVoiceMediaException();
    }
    if (prefix.length < MINIMUM_HEADER_LENGTH) {
      throw new InvalidVoiceMediaException("Voice message media is truncated.");
    }

    var valid =
        switch (normalized) {
          case "audio/webm" ->
              startsWith(prefix, EBML)
                  && containsAscii(prefix, "webm")
                  && containsAscii(prefix, "A_OPUS");
          case "audio/ogg" ->
              startsWith(prefix, "OggS".getBytes(StandardCharsets.US_ASCII))
                  && containsAscii(prefix, "OpusHead");
          case "audio/mp4" ->
              prefix.length >= 8
                  && prefix[4] == 'f'
                  && prefix[5] == 't'
                  && prefix[6] == 'y'
                  && prefix[7] == 'p'
                  && containsAscii(prefix, "mp4a");
          default -> false;
        };
    if (!valid) {
      throw new InvalidVoiceMediaException(
          "Voice message media does not match its declared format.");
    }
    return normalized;
  }

  private static boolean startsWith(byte[] content, byte[] prefix) {
    if (content.length < prefix.length) {
      return false;
    }
    for (var index = 0; index < prefix.length; index++) {
      if (content[index] != prefix[index]) {
        return false;
      }
    }
    return true;
  }

  private static boolean containsAscii(byte[] content, String marker) {
    var bytes = marker.getBytes(StandardCharsets.US_ASCII);
    for (var index = 0; index <= content.length - bytes.length; index++) {
      var matches = true;
      for (var markerIndex = 0; markerIndex < bytes.length; markerIndex++) {
        if (content[index + markerIndex] != bytes[markerIndex]) {
          matches = false;
          break;
        }
      }
      if (matches) {
        return true;
      }
    }
    return false;
  }
}
