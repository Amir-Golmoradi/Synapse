package dev.amir.synapse.call.infrastructure.adapter.in.scheduling;

import dev.amir.synapse.call.application.port.out.FindDueCallsPort;
import dev.amir.synapse.call.application.service.CallExpirationService;
import dev.amir.synapse.call.infrastructure.config.CallProperties;
import java.time.Clock;
import java.util.LinkedHashSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class CallTimeoutScheduler {
  private static final Logger LOGGER = LoggerFactory.getLogger(CallTimeoutScheduler.class);
  private final FindDueCallsPort dueCalls;
  private final CallExpirationService expirationService;
  private final CallProperties properties;
  private final Clock clock;

  public CallTimeoutScheduler(
      FindDueCallsPort dueCalls,
      CallExpirationService expirationService,
      CallProperties properties,
      Clock callClock) {
    this.dueCalls = dueCalls;
    this.expirationService = expirationService;
    this.properties = properties;
    this.clock = callClock;
  }

  @Scheduled(fixedDelayString = "${synapse.call.timeout-sweep:PT1S}")
  public void sweep() {
    var now = clock.instant();
    var ids = new LinkedHashSet<>(dueCalls.findDueCallIds(now, properties.timeoutBatchSize()));
    ids.addAll(dueCalls.findExpiredLeaseCallIds(now, properties.timeoutBatchSize()));
    for (var callId : ids) {
      try {
        expirationService.expire(callId, now);
      } catch (RuntimeException exception) {
        LOGGER.error("call_timeout_failed callId={}", callId, exception);
      }
    }
  }
}
