package dev.amir.synapse.messaging.infrastructure.adapter.out.event;

import dev.amir.synapse.messaging.application.model.MessageCommittedNotification;
import dev.amir.synapse.messaging.application.port.out.MessagePublicationPort;
import dev.amir.synapse.messaging.domain.port.in.message.MessageView;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

@Component
public class SpringMessagePublicationAdapter implements MessagePublicationPort {
  private final ApplicationEventPublisher eventPublisher;

  public SpringMessagePublicationAdapter(ApplicationEventPublisher eventPublisher) {
    this.eventPublisher = eventPublisher;
  }

  @Override
  public void publish(MessageView message) {
    eventPublisher.publishEvent(new MessageCommittedNotification(message));
  }
}
