package dev.amir.synapse.messaging.infrastructure.adapter.out.event;

import dev.amir.synapse.messaging.application.model.MessageCreatedNotification;
import dev.amir.synapse.messaging.application.port.out.MessageEventPublicationPort;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

@Component
public class SpringMessageEventPublicationAdapter implements MessageEventPublicationPort {
  private final ApplicationEventPublisher publisher;

  public SpringMessageEventPublicationAdapter(ApplicationEventPublisher publisher) {
    this.publisher = publisher;
  }

  @Override
  public void publish(MessageCreatedNotification notification) {
    publisher.publishEvent(notification);
  }
}
