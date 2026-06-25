package dev.amir.synapse.messaging.domain.port.in.create_channel_room;

@FunctionalInterface
public interface CreateChannelUseCase {
  CreateChannelResponse handle(CreateChannelCommand command);
}
