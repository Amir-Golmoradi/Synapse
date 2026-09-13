package dev.amir.synapse.messaging.infrastructure.adapter.out.event;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import dev.amir.synapse.messaging.application.model.MessageCreatedNotification;
import dev.amir.synapse.messaging.domain.enums.MessageType;
import dev.amir.synapse.messaging.domain.port.in.message.MessageView;
import dev.amir.synapse.messaging.domain.value_object.VideoMetadata;
import dev.amir.synapse.messaging.domain.value_object.VideoMetadata.VideoCodec;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.simp.SimpMessagingTemplate;

class MessageAfterCommitListenerTest {

  @Test
  void sendsVideoThroughTheExistingRoomTopic() {
    var messaging = mock(SimpMessagingTemplate.class);
    var metrics = new SimpleMeterRegistry();
    var listener = new MessageAfterCommitListener(messaging, metrics);
    var roomId = UUID.randomUUID();
    var message =
        new MessageView(
            UUID.randomUUID(),
            roomId,
            UUID.randomUUID(),
            UUID.randomUUID(),
            MessageType.VIDEO,
            null,
            new VideoMetadata("video/mp4", 100, 1_000, 16, 16, VideoCodec.H264, null),
            Instant.EPOCH);

    listener.on(new MessageCreatedNotification(UUID.randomUUID(), message, Instant.EPOCH));

    verify(messaging).convertAndSend("/topic/rooms/" + roomId, message);
    org.assertj.core.api.Assertions.assertThat(
            metrics.get("synapse.messaging.messages").tag("type", "VIDEO").counter().count())
        .isEqualTo(1);
  }
}
