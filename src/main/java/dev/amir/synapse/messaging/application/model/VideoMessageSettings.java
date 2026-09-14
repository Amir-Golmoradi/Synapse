package dev.amir.synapse.messaging.application.model;

import java.nio.file.Path;
import java.time.Duration;
import java.util.Objects;

public record VideoMessageSettings(
    Path storageRoot,
    long maxFileSize,
    Duration maxDuration,
    int maxDimension,
    long maxPixels,
    Duration probeTimeout,
    Duration stagingGrace,
    Duration orphanGrace,
    int cleanupBatchSize) {
  public VideoMessageSettings {
    Objects.requireNonNull(storageRoot, "Storage root cannot be null");
    Objects.requireNonNull(maxDuration, "Maximum duration cannot be null");
    Objects.requireNonNull(probeTimeout, "Probe timeout cannot be null");
    Objects.requireNonNull(stagingGrace, "Staging grace cannot be null");
    Objects.requireNonNull(orphanGrace, "Orphan grace cannot be null");
    if (maxFileSize <= 0
        || maxDuration.isNegative()
        || maxDuration.isZero()
        || maxDimension <= 0
        || maxPixels <= 0
        || probeTimeout.isNegative()
        || probeTimeout.isZero()
        || cleanupBatchSize <= 0) {
      throw new IllegalArgumentException("Video message settings must be positive");
    }
  }
}
