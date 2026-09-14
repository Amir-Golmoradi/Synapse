package dev.amir.synapse.messaging.infrastructure.config;

import java.nio.file.Path;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.unit.DataSize;

@ConfigurationProperties("synapse.messaging.video")
public record VideoMessageProperties(
    Path storageRoot,
    DataSize maxFileSize,
    Duration maxDuration,
    int maxDimension,
    long maxPixels,
    Duration probeTimeout,
    Duration stagingGrace,
    Duration orphanGrace,
    Duration cleanupInterval,
    int cleanupBatchSize) {
  private static final long HARD_MAX_FILE_SIZE = 25L * 1024 * 1024;
  private static final Duration HARD_MAX_DURATION = Duration.ofSeconds(60);
  private static final int HARD_MAX_DIMENSION = 1_920;
  private static final long HARD_MAX_PIXELS = 2_073_600;

  public VideoMessageProperties {
    storageRoot = storageRoot == null ? Path.of(".synapse/media") : storageRoot;
    maxFileSize = maxFileSize == null ? DataSize.ofMegabytes(25) : maxFileSize;
    maxDuration = maxDuration == null ? Duration.ofSeconds(60) : maxDuration;
    maxDimension = maxDimension <= 0 ? 1_920 : maxDimension;
    maxPixels = maxPixels <= 0 ? 2_073_600 : maxPixels;
    probeTimeout = probeTimeout == null ? Duration.ofSeconds(5) : probeTimeout;
    stagingGrace = stagingGrace == null ? Duration.ofHours(1) : stagingGrace;
    orphanGrace = orphanGrace == null ? Duration.ofHours(24) : orphanGrace;
    cleanupInterval = cleanupInterval == null ? Duration.ofHours(1) : cleanupInterval;
    cleanupBatchSize = cleanupBatchSize <= 0 ? 1_000 : cleanupBatchSize;
    if (maxFileSize.toBytes() <= 0 || maxFileSize.toBytes() > HARD_MAX_FILE_SIZE) {
      throw new IllegalArgumentException("Video file size limit must be between 1 byte and 25 MiB");
    }
    if (maxDuration.isNegative()
        || maxDuration.isZero()
        || maxDuration.compareTo(HARD_MAX_DURATION) > 0) {
      throw new IllegalArgumentException(
          "Video duration limit must be between 1 ms and 60 seconds");
    }
    if (maxDimension > HARD_MAX_DIMENSION || maxPixels > HARD_MAX_PIXELS) {
      throw new IllegalArgumentException("Video dimension limits exceed the database constraints");
    }
    if (probeTimeout.isNegative() || probeTimeout.isZero()) {
      throw new IllegalArgumentException("Video probe timeout must be positive");
    }
    if (stagingGrace.isNegative()
        || stagingGrace.isZero()
        || orphanGrace.isNegative()
        || orphanGrace.isZero()
        || cleanupInterval.isNegative()
        || cleanupInterval.isZero()) {
      throw new IllegalArgumentException("Video cleanup durations must be positive");
    }
  }
}
