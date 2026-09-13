package dev.amir.synapse.messaging.application.service;

import dev.amir.synapse.messaging.application.model.VideoMessageSettings;
import dev.amir.synapse.messaging.domain.port.out.MessageMediaReadPort;
import dev.amir.synapse.messaging.domain.port.out.MessageMediaStoragePort;
import io.micrometer.core.instrument.MeterRegistry;
import java.time.Clock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class OrphanMediaCleanupService {
  private static final Logger LOGGER = LoggerFactory.getLogger(OrphanMediaCleanupService.class);

  private final MessageMediaStoragePort storage;
  private final MessageMediaReadPort references;
  private final VideoMessageSettings settings;
  private final Clock clock;
  private final MeterRegistry meterRegistry;

  public OrphanMediaCleanupService(
      MessageMediaStoragePort storage,
      MessageMediaReadPort references,
      VideoMessageSettings settings,
      Clock messageClock,
      MeterRegistry meterRegistry) {
    this.storage = storage;
    this.references = references;
    this.settings = settings;
    this.clock = messageClock;
    this.meterRegistry = meterRegistry;
  }

  public void cleanup() {
    for (var staged :
        storage.findStaleStaging(
            clock.instant().minus(settings.stagingGrace()), settings.cleanupBatchSize())) {
      delete(staged, "staging");
    }
    for (var object :
        storage.findStaleFinal(
            clock.instant().minus(settings.orphanGrace()), settings.cleanupBatchSize())) {
      if (!references.isStorageKeyReferenced(object.storageKey())) {
        delete(object, "orphan");
      }
    }
  }

  private void delete(MessageMediaStoragePort.StoredObject object, String kind) {
    try {
      storage.delete(object);
      meterRegistry.counter("synapse.messaging.media.cleanup.deleted", "kind", kind).increment();
    } catch (RuntimeException exception) {
      meterRegistry.counter("synapse.messaging.media.cleanup.failures", "kind", kind).increment();
      LOGGER.error("message_media_cleanup_failed kind={}", kind, exception);
    }
  }
}
