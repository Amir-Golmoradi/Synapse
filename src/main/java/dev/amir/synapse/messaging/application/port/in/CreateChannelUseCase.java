package dev.amir.synapse.messaging.application.port.in;

import dev.amir.synapse.messaging.application.command.create_channel_room.CreateChannelCommand;
import dev.amir.synapse.messaging.application.command.create_channel_room.CreateChannelResponse;

@FunctionalInterface
public interface CreateChannelUseCase {
  CreateChannelResponse handle(CreateChannelCommand command);
}
