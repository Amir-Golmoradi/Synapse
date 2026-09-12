package dev.amir.synapse.messaging.domain.port.in.get_voice_media;

import org.jspecify.annotations.Nullable;

public record MediaRangeRequest(
    @Nullable Long start, @Nullable Long end, @Nullable Long suffixLength) {

  public MediaRangeRequest {
    var closedOrOpen = start != null && suffixLength == null;
    var suffix = start == null && end == null && suffixLength != null;
    if ((!closedOrOpen && !suffix)
        || (start != null && start < 0)
        || (end != null && (end < 0 || end < start))
        || (suffixLength != null && suffixLength <= 0)) {
      throw new IllegalArgumentException("Invalid media byte range");
    }
  }
}
