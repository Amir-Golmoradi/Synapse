package dev.amir.synapse.call.application.service;

import dev.amir.synapse.call.application.model.CallLifecycleNotification;
import dev.amir.synapse.call.application.port.out.CallEventPublicationPort;
import dev.amir.synapse.call.domain.model.Call;
import dev.amir.synapse.call.domain.port.in.CallView;
import java.time.Clock;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class CallNotifications {
  private final CallEventPublicationPort publisher;
  private final Clock clock;

  public CallNotifications(CallEventPublicationPort publisher, Clock callClock) {
    this.publisher = publisher;
    this.clock = callClock;
  }

  public void publish(String type, Call call, int generation) {
    publisher.publish(
        new CallLifecycleNotification(
            UUID.randomUUID(), type, CallView.from(call, generation), clock.instant()));
  }
}
