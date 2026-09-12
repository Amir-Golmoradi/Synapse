package dev.amir.synapse.messaging.infrastructure.adapter.in.web.rest;

import dev.amir.synapse.messaging.domain.exception.InvalidMediaRangeException;
import dev.amir.synapse.messaging.domain.port.in.get_voice_media.MediaRangeRequest;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Component;

@Component
public class HttpByteRangeParser {
  public @Nullable MediaRangeRequest parse(@Nullable String header) {
    if (header == null) {
      return null;
    }
    if (!header.startsWith("bytes=") || header.indexOf(',') >= 0) {
      throw new InvalidMediaRangeException();
    }
    var value = header.substring("bytes=".length()).strip();
    var separator = value.indexOf('-');
    if (separator < 0 || separator != value.lastIndexOf('-')) {
      throw new InvalidMediaRangeException();
    }
    var first = value.substring(0, separator).strip();
    var last = value.substring(separator + 1).strip();
    try {
      if (first.isEmpty()) {
        if (last.isEmpty()) {
          throw new InvalidMediaRangeException();
        }
        return new MediaRangeRequest(null, null, Long.parseLong(last));
      }
      return new MediaRangeRequest(
          Long.parseLong(first), last.isEmpty() ? null : Long.parseLong(last), null);
    } catch (IllegalArgumentException exception) {
      throw new InvalidMediaRangeException(exception);
    }
  }
}
