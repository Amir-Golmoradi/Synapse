package dev.amir.synapse.messaging.infrastructure.adapter.out.event;

import dev.amir.synapse.messaging.application.model.MessageCreatedNotification;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class MessageAfterCommitListener {
  private static final Logger LOGGER = LoggerFactory.getLogger(MessageAfterCommitListener.class);
  private static final String ROOM_TOPIC_PREFIX = "/topic/rooms/";

  private final SimpMessagingTemplate messagingTemplate;
  private final MeterRegistry meterRegistry;

  public MessageAfterCommitListener(
      SimpMessagingTemplate messagingTemplate, MeterRegistry meterRegistry) {
    this.messagingTemplate = messagingTemplate;
    this.meterRegistry = meterRegistry;
  }

  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
  public void on(MessageCreatedNotification notification) {
    var message = notification.message();
    try {
      messagingTemplate.convertAndSend(ROOM_TOPIC_PREFIX + message.roomId(), message);
      meterRegistry
          .counter("synapse.messaging.messages", "type", message.type().name())
          .increment();
    } catch (RuntimeException exception) {
      meterRegistry.counter("synapse.messaging.delivery.failures").increment();
      if (LOGGER.isErrorEnabled()) {
        LOGGER.error(
            "message_notification_failed messageId={} roomId={} eventId={}",
            message.messageId(),
            message.roomId(),
            notification.eventId(),
            exception);
      }
    }
  }
}
