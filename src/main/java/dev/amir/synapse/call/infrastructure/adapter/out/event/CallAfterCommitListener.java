package dev.amir.synapse.call.infrastructure.adapter.out.event;

import dev.amir.synapse.call.application.model.CallLifecycleNotification;
import io.micrometer.core.instrument.MeterRegistry;
import java.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class CallAfterCommitListener {
  private static final Logger LOGGER = LoggerFactory.getLogger(CallAfterCommitListener.class);
  private final SimpMessagingTemplate messagingTemplate;
  private final MeterRegistry meterRegistry;

  public CallAfterCommitListener(
      SimpMessagingTemplate messagingTemplate, MeterRegistry meterRegistry) {
    this.messagingTemplate = messagingTemplate;
    this.meterRegistry = meterRegistry;
  }

  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
  public void on(CallLifecycleNotification notification) {
    var call = notification.call();
    try {
      messagingTemplate.convertAndSendToUser(
          call.callerId().toString(), "/queue/calls/events", notification);
      messagingTemplate.convertAndSendToUser(
          call.calleeId().toString(), "/queue/calls/events", notification);
      LOGGER.info(
          "call_lifecycle callId={} eventId={} callerId={} calleeId={} status={} version={}"
              + " generation={}",
          call.callId(),
          notification.eventId(),
          call.callerId(),
          call.calleeId(),
          call.status(),
          call.version(),
          call.negotiationGeneration());
      meterRegistry.counter("synapse.calls.lifecycle", "status", call.status().name()).increment();
      if ("CALL_ACTIVE".equals(notification.type())
          && call.connectedAt() != null
          && call.acceptedAt() != null) {
        meterRegistry
            .timer("synapse.calls.connection.setup")
            .record(Duration.between(call.acceptedAt(), call.connectedAt()));
      }
    } catch (RuntimeException exception) {
      meterRegistry.counter("synapse.calls.delivery.failures").increment();
      LOGGER.error(
          "call_notification_failed callId={} eventId={}",
          call.callId(),
          notification.eventId(),
          exception);
    }
  }
}
