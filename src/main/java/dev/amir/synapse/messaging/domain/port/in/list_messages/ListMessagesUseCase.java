package dev.amir.synapse.messaging.domain.port.in.list_messages;

@FunctionalInterface
public interface ListMessagesUseCase {
  ListMessagesResult handle(ListMessagesQuery query);
}
