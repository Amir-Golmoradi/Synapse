package dev.amir.synapse.messaging.application.service;

import dev.amir.synapse.messaging.application.model.VoiceMediaCleanupSettings;
import dev.amir.synapse.messaging.application.port.out.VoiceMediaStoragePort;
import dev.amir.synapse.messaging.domain.port.out.VoiceMessageMediaPort;
import io.micrometer.core.instrument.MeterRegistry;
import java.time.Clock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

@Service
public class VoiceMediaCleanupService {
  private static final Logger LOGGER = LoggerFactory.getLogger(VoiceMediaCleanupService.class);

  private final VoiceMediaStoragePort storagePort;
  private final VoiceMessageMediaPort mediaPort;
  private final VoiceMediaCleanupSettings settings;
  private final Clock clock;
  private final MeterRegistry meterRegistry;

  public VoiceMediaCleanupService(
      VoiceMediaStoragePort storagePort,
      VoiceMessageMediaPort mediaPort,
      VoiceMediaCleanupSettings settings,
      @Qualifier("voiceMessageClock") Clock clock,
      MeterRegistry meterRegistry) {
    this.storagePort = storagePort;
    this.mediaPort = mediaPort;
    this.settings = settings;
    this.clock = clock;
    this.meterRegistry = meterRegistry;
  }

  public void clean() {
    var now = clock.instant();
    var partialCutoff = now.minus(settings.partialCleanupAge());
    var orphanCutoff = now.minus(settings.orphanCleanupAge());
    for (var object : storagePort.listOlderThan(partialCutoff)) {
      var removable =
          object.partial()
              || (object.lastModified().isBefore(orphanCutoff)
                  && !mediaPort.isStorageKeyReferenced(object.storageKey()));
      if (!removable) {
        continue;
      }
      try {
        storagePort.delete(object.storageKey());
        meterRegistry
            .counter(
                "synapse.voice.messages.cleanup.deleted",
                "kind",
                object.partial() ? "partial" : "orphan")
            .increment();
      } catch (RuntimeException exception) {
        meterRegistry.counter("synapse.voice.messages.cleanup.failures").increment();
        if (LOGGER.isErrorEnabled()) {
          LOGGER.error("voice_media_cleanup_failed partial={}", object.partial(), exception);
        }
      }
    }
  }
}
