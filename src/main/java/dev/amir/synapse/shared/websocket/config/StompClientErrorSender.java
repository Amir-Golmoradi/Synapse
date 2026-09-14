package dev.amir.synapse.shared.websocket.config;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Lazy;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;

@Component
public class StompClientErrorSender {
  private final MessageChannel clientOutboundChannel;

  public StompClientErrorSender(
      @Lazy @Qualifier("clientOutboundChannel") MessageChannel clientOutboundChannel) {
    this.clientOutboundChannel = clientOutboundChannel;
  }

  public void reject(StompHeaderAccessor request) {
    var sessionId = request.getSessionId();
    if (sessionId == null) {
      return;
    }
    var error = StompHeaderAccessor.create(StompCommand.ERROR);
    error.setSessionId(sessionId);
    error.setMessage(SanitizedStompErrorHandler.CLIENT_ERROR_MESSAGE);
    var receipt = request.getReceipt();
    if (receipt != null) {
      error.setReceiptId(receipt);
    }
    error.setLeaveMutable(true);
    clientOutboundChannel.send(
        MessageBuilder.createMessage(new byte[0], error.getMessageHeaders()));
  }
}
