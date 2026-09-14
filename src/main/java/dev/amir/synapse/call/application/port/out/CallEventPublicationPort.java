package dev.amir.synapse.call.application.port.out;

import dev.amir.synapse.call.application.model.CallLifecycleNotification;

@FunctionalInterface
public interface CallEventPublicationPort {
  void publish(CallLifecycleNotification notification);
}
