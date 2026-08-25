package dev.amir.synapse.messaging.domain.port.in.send_message;

import dev.amir.synapse.messaging.domain.port.in.message.MessageView;

@FunctionalInterface
public interface SendMessageUseCase {
  MessageView handle(SendMessageCommand command);
}
