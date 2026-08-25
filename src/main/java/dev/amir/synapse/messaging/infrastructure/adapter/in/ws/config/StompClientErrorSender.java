package dev.amir.synapse.messaging.infrastructure.adapter.in.ws.config;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Lazy;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;

@Component
class StompClientErrorSender {
  private final MessageChannel clientOutboundChannel;

  StompClientErrorSender(
      @Lazy @Qualifier("clientOutboundChannel") MessageChannel clientOutboundChannel) {
    this.clientOutboundChannel = clientOutboundChannel;
  }

  void reject(StompHeaderAccessor request) {
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
