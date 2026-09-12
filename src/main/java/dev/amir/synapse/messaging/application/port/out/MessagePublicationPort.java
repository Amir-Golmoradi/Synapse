package dev.amir.synapse.messaging.application.port.out;

import dev.amir.synapse.messaging.domain.port.in.message.MessageView;

@FunctionalInterface
public interface MessagePublicationPort {
  void publish(MessageView message);
}
