package dev.amir.synapse.messaging.infrastructure.adapter.in.scheduling;

import dev.amir.synapse.messaging.application.service.OrphanMediaCleanupService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class OrphanMediaCleanupScheduler {
  private final OrphanMediaCleanupService cleanupService;

  public OrphanMediaCleanupScheduler(OrphanMediaCleanupService cleanupService) {
    this.cleanupService = cleanupService;
  }

  @Scheduled(fixedDelayString = "${synapse.messaging.video.cleanup-interval:1h}")
  public void cleanup() {
    cleanupService.cleanup();
  }
}
