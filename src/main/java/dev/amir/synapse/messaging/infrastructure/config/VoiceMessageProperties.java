package dev.amir.synapse.messaging.infrastructure.config;

import java.nio.file.Path;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("synapse.messaging.voice")
public record VoiceMessageProperties(
    Path storageRoot,
    Duration partialCleanupAge,
    Duration orphanCleanupAge,
    Duration cleanupInterval) {
  public VoiceMessageProperties {
    storageRoot = storageRoot == null ? Path.of("./var/voice-messages") : storageRoot;
    partialCleanupAge = defaultDuration(partialCleanupAge, Duration.ofHours(1));
    orphanCleanupAge = defaultDuration(orphanCleanupAge, Duration.ofHours(24));
    cleanupInterval = defaultDuration(cleanupInterval, Duration.ofHours(1));
    requirePositive(partialCleanupAge, "partial-cleanup-age");
    requirePositive(orphanCleanupAge, "orphan-cleanup-age");
    requirePositive(cleanupInterval, "cleanup-interval");
    if (orphanCleanupAge.compareTo(partialCleanupAge) < 0) {
      throw new IllegalArgumentException(
          "orphan-cleanup-age must not be shorter than partial-cleanup-age");
    }
  }

  private static Duration defaultDuration(Duration value, Duration fallback) {
    return value == null ? fallback : value;
  }

  private static void requirePositive(Duration value, String name) {
    if (value.isZero() || value.isNegative()) {
      throw new IllegalArgumentException(name + " must be positive");
    }
  }
}
