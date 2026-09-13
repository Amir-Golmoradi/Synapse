package dev.amir.synapse.messaging.application.service;

import dev.amir.synapse.messaging.domain.port.out.MessagePersistenceResult;
import dev.amir.synapse.messaging.domain.port.out.MessageWritePort;
import dev.amir.synapse.messaging.domain.value_object.VideoMetadata;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class VideoMessageTransactionService {
  private final MessageWritePort messages;
  private final MessageNotifications notifications;

  public VideoMessageTransactionService(
      MessageWritePort messages, MessageNotifications notifications) {
    this.messages = messages;
    this.notifications = notifications;
  }

  @Transactional
  public MessagePersistenceResult create(
      UUID roomId,
      UUID senderId,
      UUID clientMessageId,
      String storageKey,
      byte[] contentSha256,
      VideoMetadata metadata) {
    var result =
        messages.saveVideoAuthorized(
            roomId, senderId, clientMessageId, storageKey, contentSha256, metadata);
    if (result.created()) {
      notifications.publish(result.message());
    }
    return result;
  }
}
