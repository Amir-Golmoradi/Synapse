package dev.amir.synapse.messaging.infrastructure.adapter.in.ws.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageHeaderAccessor;

class StompClientErrorSenderTest {

  @Test
  void sendsASanitizedErrorToTheRejectedSession() {
    var outboundChannel = mock(MessageChannel.class);
    var sender = new StompClientErrorSender(outboundChannel);
    var request = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
    request.setSessionId("session-1");
    request.setReceipt("receipt-1");

    sender.reject(request);

    var messageCaptor = ArgumentCaptor.forClass(Message.class);
    verify(outboundChannel).send(messageCaptor.capture());
    var error =
        MessageHeaderAccessor.getAccessor(messageCaptor.getValue(), StompHeaderAccessor.class);
    assertThat(error).isNotNull();
    assertThat(error.getCommand()).isEqualTo(StompCommand.ERROR);
    assertThat(error.getSessionId()).isEqualTo("session-1");
    assertThat(error.getReceiptId()).isEqualTo("receipt-1");
    assertThat(error.getMessage()).isEqualTo("STOMP request rejected");
  }
}
