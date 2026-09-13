package dev.amir.synapse.messaging.application.service;

import dev.amir.synapse.messaging.application.model.MessageCreatedNotification;
import dev.amir.synapse.messaging.application.port.out.MessageEventPublicationPort;
import dev.amir.synapse.messaging.domain.port.in.message.MessageView;
import java.time.Clock;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class MessageNotifications {
  private final MessageEventPublicationPort publisher;
  private final Clock clock;

  public MessageNotifications(MessageEventPublicationPort publisher, Clock messageClock) {
    this.publisher = publisher;
    this.clock = messageClock;
  }

  public void publish(MessageView message) {
    publisher.publish(new MessageCreatedNotification(UUID.randomUUID(), message, clock.instant()));
  }
}
