package dev.amir.synapse.messaging.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import dev.amir.synapse.messaging.domain.enums.MessageType;
import dev.amir.synapse.messaging.domain.port.in.message.MessageView;
import dev.amir.synapse.messaging.domain.port.out.MessagePersistenceResult;
import dev.amir.synapse.messaging.domain.port.out.MessageWritePort;
import dev.amir.synapse.messaging.domain.value_object.VideoMetadata;
import dev.amir.synapse.messaging.domain.value_object.VideoMetadata.VideoCodec;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class VideoMessageTransactionServiceTest {

  @Test
  void publishesOnlyNewlyCreatedMessages() {
    var messages = mock(MessageWritePort.class);
    var notifications = mock(MessageNotifications.class);
    var service = new VideoMessageTransactionService(messages, notifications);
    var roomId = UUID.randomUUID();
    var senderId = UUID.randomUUID();
    var clientId = UUID.randomUUID();
    var metadata = new VideoMetadata("video/mp4", 10, 100, 16, 16, VideoCodec.H264, null);
    var message =
        new MessageView(
            UUID.randomUUID(),
            roomId,
            senderId,
            clientId,
            MessageType.VIDEO,
            null,
            metadata,
            Instant.EPOCH);
    var created = new MessagePersistenceResult(message, true);
    when(messages.saveVideoAuthorized(
            roomId, senderId, clientId, "video/aa/id.mp4", new byte[32], metadata))
        .thenReturn(created);

    assertThat(
            service.create(roomId, senderId, clientId, "video/aa/id.mp4", new byte[32], metadata))
        .isEqualTo(created);
    verify(notifications).publish(message);

    var retry = new MessagePersistenceResult(message, false);
    when(messages.saveVideoAuthorized(
            roomId, senderId, clientId, "video/aa/retry.mp4", new byte[32], metadata))
        .thenReturn(retry);
    assertThat(
            service.create(
                roomId, senderId, clientId, "video/aa/retry.mp4", new byte[32], metadata))
        .isEqualTo(retry);
    verify(notifications, never()).publish(null);
    verify(notifications).publish(message);
  }
}
