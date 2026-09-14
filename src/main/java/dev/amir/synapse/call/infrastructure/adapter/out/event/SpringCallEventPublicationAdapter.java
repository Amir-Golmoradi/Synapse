package dev.amir.synapse.call.infrastructure.adapter.out.event;

import dev.amir.synapse.call.application.model.CallLifecycleNotification;
import dev.amir.synapse.call.application.port.out.CallEventPublicationPort;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

@Component
public class SpringCallEventPublicationAdapter implements CallEventPublicationPort {
  private final ApplicationEventPublisher publisher;

  public SpringCallEventPublicationAdapter(ApplicationEventPublisher publisher) {
    this.publisher = publisher;
  }

  @Override
  public void publish(CallLifecycleNotification notification) {
    publisher.publishEvent(notification);
  }
}
