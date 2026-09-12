package dev.amir.synapse.messaging.infrastructure.adapter.in.scheduling;

import dev.amir.synapse.messaging.application.service.VoiceMediaCleanupService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class VoiceMediaCleanupScheduler {
  private static final Logger LOGGER = LoggerFactory.getLogger(VoiceMediaCleanupScheduler.class);
  private final VoiceMediaCleanupService cleanupService;

  public VoiceMediaCleanupScheduler(VoiceMediaCleanupService cleanupService) {
    this.cleanupService = cleanupService;
  }

  @Scheduled(
      fixedDelayString = "${synapse.messaging.voice.cleanup-interval:1h}",
      initialDelayString = "${synapse.messaging.voice.cleanup-interval:1h}")
  public void clean() {
    try {
      cleanupService.clean();
    } catch (RuntimeException exception) {
      LOGGER.error("voice_media_cleanup_scan_failed", exception);
    }
  }
}
