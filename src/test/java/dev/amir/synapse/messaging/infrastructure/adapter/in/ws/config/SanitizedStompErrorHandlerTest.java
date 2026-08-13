package dev.amir.synapse.messaging.infrastructure.adapter.in.ws.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.messaging.Message;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.messaging.support.MessageHeaderAccessor;

class SanitizedStompErrorHandlerTest {

  private final SanitizedStompErrorHandler errorHandler = new SanitizedStompErrorHandler();

  @Test
  void sanitizesClientProcessingFailuresAndPreservesTheReceipt() {
    var clientAccessor = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
    clientAccessor.setReceipt("client-receipt");
    var clientMessage = message(clientAccessor, "client-payload".getBytes());

    var errorMessage =
        errorHandler.handleClientMessageProcessingError(
            clientMessage, new IllegalStateException("sensitive server detail"));
    var errorAccessor = MessageHeaderAccessor.getAccessor(errorMessage, StompHeaderAccessor.class);

    assertThat(errorAccessor).isNotNull();
    assertThat(errorAccessor.getCommand()).isEqualTo(StompCommand.ERROR);
    assertThat(errorAccessor.getMessage()).isEqualTo("STOMP request rejected");
    assertThat(errorAccessor.getReceiptId()).isEqualTo("client-receipt");
    assertThat(errorMessage.getPayload()).isEmpty();
    assertThat(errorMessage.toString()).doesNotContain("sensitive server detail");
  }

  @Test
  void sanitizesErrorFramesProducedElsewhereInTheMessagingPipeline() {
    var sourceAccessor = StompHeaderAccessor.create(StompCommand.ERROR);
    sourceAccessor.setMessage("sensitive downstream detail");
    sourceAccessor.setReceiptId("server-receipt");
    var source = message(sourceAccessor, "sensitive body".getBytes());

    var errorMessage = errorHandler.handleErrorMessageToClient(source);
    var errorAccessor = MessageHeaderAccessor.getAccessor(errorMessage, StompHeaderAccessor.class);

    assertThat(errorAccessor).isNotNull();
    assertThat(errorAccessor.getMessage()).isEqualTo("STOMP request rejected");
    assertThat(errorAccessor.getReceiptId()).isEqualTo("server-receipt");
    assertThat(errorMessage.getPayload()).isEmpty();
    assertThat(errorMessage.toString())
        .doesNotContain("sensitive downstream detail", "sensitive body");
  }

  private static Message<byte[]> message(StompHeaderAccessor accessor, byte[] payload) {
    accessor.setLeaveMutable(true);
    return MessageBuilder.createMessage(payload, accessor.getMessageHeaders());
  }
}
