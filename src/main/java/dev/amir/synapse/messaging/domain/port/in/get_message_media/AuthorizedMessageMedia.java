package dev.amir.synapse.messaging.domain.port.in.get_message_media;

import java.nio.file.Path;
import java.util.Arrays;

public record AuthorizedMessageMedia(Path path, String contentType, long sizeBytes, byte[] sha256) {
  public AuthorizedMessageMedia {
    sha256 = Arrays.copyOf(sha256, sha256.length);
  }

  @Override
  public byte[] sha256() {
    return Arrays.copyOf(sha256, sha256.length);
  }
}
