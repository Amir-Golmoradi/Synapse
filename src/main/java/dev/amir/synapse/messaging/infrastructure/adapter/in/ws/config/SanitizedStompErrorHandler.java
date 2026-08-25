package dev.amir.synapse.messaging.infrastructure.adapter.in.ws.config;

import org.jspecify.annotations.Nullable;
import org.springframework.messaging.Message;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.StompSubProtocolErrorHandler;

@Component
public class SanitizedStompErrorHandler extends StompSubProtocolErrorHandler {
  static final String CLIENT_ERROR_MESSAGE = "STOMP request rejected";

  @Override
  public Message<byte[]> handleClientMessageProcessingError(
      @Nullable Message<byte[]> clientMessage, Throwable exception) {
    return createSanitizedError(clientMessage, true);
  }

  @Override
  public Message<byte[]> handleErrorMessageToClient(Message<byte[]> errorMessage) {
    return createSanitizedError(errorMessage, false);
  }

  private static Message<byte[]> createSanitizedError(
      @Nullable Message<byte[]> source, boolean clientMessage) {
    var errorAccessor = StompHeaderAccessor.create(StompCommand.ERROR);
    errorAccessor.setMessage(CLIENT_ERROR_MESSAGE);
    errorAccessor.setLeaveMutable(true);

    if (source != null) {
      var sourceAccessor = MessageHeaderAccessor.getAccessor(source, StompHeaderAccessor.class);
      if (sourceAccessor != null) {
        var receiptId = clientMessage ? sourceAccessor.getReceipt() : sourceAccessor.getReceiptId();
        if (receiptId != null) {
          errorAccessor.setReceiptId(receiptId);
        }
      }
    }

    return MessageBuilder.createMessage(new byte[0], errorAccessor.getMessageHeaders());
  }
}
