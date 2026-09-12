package dev.amir.synapse.messaging.infrastructure.adapter.out.event;

import dev.amir.synapse.messaging.application.model.MessageCommittedNotification;
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
  public void on(MessageCommittedNotification notification) {
    var message = notification.message();
    try {
      messagingTemplate.convertAndSend(ROOM_TOPIC_PREFIX + message.roomId(), message);
      if (LOGGER.isInfoEnabled()) {
        LOGGER.info(
            "message_committed messageId={} roomId={} senderId={} clientMessageId={} type={}",
            message.messageId(),
            message.roomId(),
            message.senderId(),
            message.clientMessageId(),
            message.type());
      }
      meterRegistry
          .counter("synapse.messages.committed", "type", message.type().name())
          .increment();
    } catch (RuntimeException exception) {
      meterRegistry.counter("synapse.messages.delivery.failures").increment();
      if (LOGGER.isErrorEnabled()) {
        LOGGER.error(
            "message_delivery_failed messageId={} roomId={}",
            message.messageId(),
            message.roomId(),
            exception);
      }
    }
  }
}
