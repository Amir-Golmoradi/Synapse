package dev.amir.synapse.messaging.infrastructure.adapter.in.ws.message;

import dev.amir.synapse.messaging.domain.exception.MessageValidationException;
import dev.amir.synapse.messaging.domain.port.in.message.MessageView;
import dev.amir.synapse.messaging.domain.port.in.send_message.SendMessageCommand;
import dev.amir.synapse.messaging.domain.port.in.send_message.SendMessageUseCase;
import dev.amir.synapse.shared.domain.DomainException;
import java.security.Principal;
import java.util.Objects;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

@Controller
public class MessageStompController {
  private static final Logger LOGGER = LoggerFactory.getLogger(MessageStompController.class);
  private static final String ROOM_TOPIC_PREFIX = "/topic/rooms/";

  private final SendMessageUseCase sendMessageUseCase;
  private final SimpMessagingTemplate messagingTemplate;

  public MessageStompController(
      SendMessageUseCase sendMessageUseCase, SimpMessagingTemplate messagingTemplate) {
    this.sendMessageUseCase = sendMessageUseCase;
    this.messagingTemplate = messagingTemplate;
  }

  @MessageMapping("/rooms/{roomId}/messages")
  public void send(
      @DestinationVariable UUID roomId, @Payload SendMessageRequest request, Principal principal) {
    Objects.requireNonNull(request, "Message request cannot be null");
    var senderId = UUID.fromString(principal.getName());

    MessageView message;
    try {
      if (request.clientMessageId() == null) {
        throw new MessageValidationException("Client message ID cannot be null.");
      }
      message =
          sendMessageUseCase.handle(
              new SendMessageCommand(senderId, roomId, request.clientMessageId(), request.text()));
    } catch (DomainException exception) {
      throw new RecoverableMessageException(exception, roomId, request.clientMessageId());
    }

    publishAfterCommit(message);
  }

  private void publishAfterCommit(MessageView message) {
    try {
      messagingTemplate.convertAndSend(ROOM_TOPIC_PREFIX + message.roomId(), message);
    } catch (RuntimeException exception) {
      if (LOGGER.isErrorEnabled()) {
        LOGGER.error(
            "Committed message {} could not be published to room {}",
            message.messageId(),
            message.roomId(),
            exception);
      }
    }
  }
}
