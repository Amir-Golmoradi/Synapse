package dev.amir.synapse.messaging.domain.port.out;

import dev.amir.synapse.messaging.domain.port.in.message.MessageView;
import dev.amir.synapse.messaging.domain.value_object.VideoMetadata;
import java.util.UUID;

public interface MessageWritePort {
  MessageView saveAuthorized(UUID roomId, UUID senderId, UUID clientMessageId, String text);

  MessagePersistenceResult saveVideoAuthorized(
      UUID roomId,
      UUID senderId,
      UUID clientMessageId,
      String storageKey,
      byte[] contentSha256,
      VideoMetadata metadata);
}
