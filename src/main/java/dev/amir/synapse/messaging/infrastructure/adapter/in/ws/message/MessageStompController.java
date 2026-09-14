package dev.amir.synapse.messaging.infrastructure.adapter.in.ws.message;

import dev.amir.synapse.messaging.domain.exception.MessageValidationException;
import dev.amir.synapse.messaging.domain.port.in.send_message.SendMessageCommand;
import dev.amir.synapse.messaging.domain.port.in.send_message.SendMessageUseCase;
import dev.amir.synapse.shared.domain.DomainException;
import java.security.Principal;
import java.util.Objects;
import java.util.UUID;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Controller;

@Controller
public class MessageStompController {
  private final SendMessageUseCase sendMessageUseCase;

  public MessageStompController(SendMessageUseCase sendMessageUseCase) {
    this.sendMessageUseCase = sendMessageUseCase;
  }

  @MessageMapping("/rooms/{roomId}/messages")
  public void send(
      @DestinationVariable UUID roomId, @Payload SendMessageRequest request, Principal principal) {
    Objects.requireNonNull(request, "Message request cannot be null");
    var senderId = UUID.fromString(principal.getName());

    try {
      if (request.clientMessageId() == null) {
        throw new MessageValidationException("Client message ID cannot be null.");
      }
      sendMessageUseCase.handle(
          new SendMessageCommand(senderId, roomId, request.clientMessageId(), request.text()));
    } catch (DomainException exception) {
      throw new RecoverableMessageException(exception, roomId, request.clientMessageId());
    }
  }
}
