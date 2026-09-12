package dev.amir.synapse.messaging.infrastructure.adapter.out.event;

import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import dev.amir.synapse.messaging.application.model.MessageCommittedNotification;
import dev.amir.synapse.messaging.domain.port.in.message.MessageView;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.MessageDeliveryException;
import org.springframework.messaging.simp.SimpMessagingTemplate;

class MessageAfterCommitListenerTest {
  private final SimpMessagingTemplate messagingTemplate = mock(SimpMessagingTemplate.class);
  private final SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
  private final MessageAfterCommitListener listener =
      new MessageAfterCommitListener(messagingTemplate, meterRegistry);

  @Test
  void publishesCanonicalMessageToExistingRoomTopic() {
    var message = message();

    listener.on(new MessageCommittedNotification(message));

    verify(messagingTemplate).convertAndSend("/topic/rooms/" + message.roomId(), message);
  }

  @Test
  void brokerFailureDoesNotEscapeAfterCommitListener() {
    var message = message();
    doThrow(new MessageDeliveryException("broker unavailable"))
        .when(messagingTemplate)
        .convertAndSend("/topic/rooms/" + message.roomId(), message);

    assertThatNoException()
        .isThrownBy(() -> listener.on(new MessageCommittedNotification(message)));
  }

  private static MessageView message() {
    return new MessageView(
        UUID.randomUUID(),
        UUID.randomUUID(),
        UUID.randomUUID(),
        UUID.randomUUID(),
        "hello",
        Instant.parse("2026-09-12T12:00:00Z"));
  }
}
