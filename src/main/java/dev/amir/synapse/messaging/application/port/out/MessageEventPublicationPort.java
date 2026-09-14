package dev.amir.synapse.messaging.application.port.out;

import dev.amir.synapse.messaging.application.model.MessageCreatedNotification;

@FunctionalInterface
public interface MessageEventPublicationPort {
  void publish(MessageCreatedNotification notification);
}
