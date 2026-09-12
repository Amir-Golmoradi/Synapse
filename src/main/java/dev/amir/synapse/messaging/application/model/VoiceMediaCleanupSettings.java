package dev.amir.synapse.messaging.application.model;

import java.time.Duration;
import java.util.Objects;

public record VoiceMediaCleanupSettings(Duration partialCleanupAge, Duration orphanCleanupAge) {
  public VoiceMediaCleanupSettings {
    Objects.requireNonNull(partialCleanupAge, "Partial cleanup age cannot be null");
    Objects.requireNonNull(orphanCleanupAge, "Orphan cleanup age cannot be null");
  }
}
