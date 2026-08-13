package dev.amir.synapse.messaging.domain.port.out;

import dev.amir.synapse.messaging.domain.port.in.message.MessageView;
import java.util.UUID;

@FunctionalInterface
public interface MessageWritePort {
  MessageView saveAuthorized(UUID roomId, UUID senderId, UUID clientMessageId, String text);
}
