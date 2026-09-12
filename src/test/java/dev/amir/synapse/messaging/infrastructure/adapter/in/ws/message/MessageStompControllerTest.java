package dev.amir.synapse.messaging.infrastructure.adapter.in.ws.message;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import dev.amir.synapse.messaging.domain.exception.MessageIdempotencyConflictException;
import dev.amir.synapse.messaging.domain.exception.MessageRoomAccessDeniedException;
import dev.amir.synapse.messaging.domain.exception.MessageValidationException;
import dev.amir.synapse.messaging.domain.port.in.message.MessageView;
import dev.amir.synapse.messaging.domain.port.in.send_message.SendMessageCommand;
import dev.amir.synapse.messaging.domain.port.in.send_message.SendMessageUseCase;
import java.security.Principal;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.messaging.MessageDeliveryException;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.annotation.SendToUser;

class MessageStompControllerTest {
  private final SendMessageUseCase sendMessageUseCase = mock(SendMessageUseCase.class);
  private final SimpMessagingTemplate messagingTemplate = mock(SimpMessagingTemplate.class);
  private final MessageStompController controller =
      new MessageStompController(sendMessageUseCase, messagingTemplate);

  @Test
  void sendsAsAuthenticatedPrincipalAndPublishesCanonicalMessage() {
    var senderId = UUID.randomUUID();
    var roomId = UUID.randomUUID();
    var clientMessageId = UUID.randomUUID();
    var message = message(roomId, senderId, clientMessageId, "Hello");
    when(sendMessageUseCase.handle(org.mockito.ArgumentMatchers.any())).thenReturn(message);

    controller.send(roomId, new SendMessageRequest(clientMessageId, "Hello"), principal(senderId));

    var command = ArgumentCaptor.forClass(SendMessageCommand.class);
    verify(sendMessageUseCase).handle(command.capture());
    assertThat(command.getValue())
        .satisfies(
            sent -> {
              assertThat(sent.senderId()).isEqualTo(senderId);
              assertThat(sent.roomId()).isEqualTo(roomId);
              assertThat(sent.clientMessageId()).isEqualTo(clientMessageId);
              assertThat(sent.text()).isEqualTo("Hello");
            });
    verify(messagingTemplate).convertAndSend("/topic/rooms/" + roomId, message);
  }

  @Test
  void exactRetryRepublishesTheSameCanonicalMessage() {
    var senderId = UUID.randomUUID();
    var roomId = UUID.randomUUID();
    var clientMessageId = UUID.randomUUID();
    var request = new SendMessageRequest(clientMessageId, "Retry me");
    var message = message(roomId, senderId, clientMessageId, "Retry me");
    when(sendMessageUseCase.handle(org.mockito.ArgumentMatchers.any())).thenReturn(message);

    controller.send(roomId, request, principal(senderId));
    controller.send(roomId, request, principal(senderId));

    verify(sendMessageUseCase, org.mockito.Mockito.times(2))
        .handle(org.mockito.ArgumentMatchers.any());
    verify(messagingTemplate, org.mockito.Mockito.times(2))
        .convertAndSend("/topic/rooms/" + roomId, message);
  }

  @Test
  void committedMessageIsNotReportedAsFailedWhenBroadcastFails() {
    var senderId = UUID.randomUUID();
    var roomId = UUID.randomUUID();
    var clientMessageId = UUID.randomUUID();
    var message = message(roomId, senderId, clientMessageId, "Persisted");
    when(sendMessageUseCase.handle(org.mockito.ArgumentMatchers.any())).thenReturn(message);
    doThrow(new MessageDeliveryException("broker unavailable"))
        .when(messagingTemplate)
        .convertAndSend("/topic/rooms/" + roomId, message);

    assertThatNoException()
        .isThrownBy(
            () ->
                controller.send(
                    roomId,
                    new SendMessageRequest(clientMessageId, "Persisted"),
                    principal(senderId)));
  }

  @Test
  void validationFailureIsRecoverableAndIsNotPublished() {
    var senderId = UUID.randomUUID();
    var roomId = UUID.randomUUID();
    var clientMessageId = UUID.randomUUID();

    assertThatThrownBy(
            () ->
                controller.send(
                    roomId, new SendMessageRequest(clientMessageId, "   "), principal(senderId)))
        .isInstanceOf(RecoverableMessageException.class)
        .satisfies(
            thrown ->
                assertThat(((RecoverableMessageException) thrown).getDomainException())
                    .isInstanceOf(MessageValidationException.class));

    verifyNoInteractions(sendMessageUseCase, messagingTemplate);
  }

  @Test
  void missingClientMessageIdIsRecoverableAndIsNotPublished() {
    var senderId = UUID.randomUUID();
    var roomId = UUID.randomUUID();

    assertThatThrownBy(
            () ->
                controller.send(roomId, new SendMessageRequest(null, "Hello"), principal(senderId)))
        .isInstanceOf(RecoverableMessageException.class)
        .satisfies(
            thrown ->
                assertThat(((RecoverableMessageException) thrown).getDomainException())
                    .isInstanceOf(MessageValidationException.class));

    verifyNoInteractions(sendMessageUseCase, messagingTemplate);
  }

  @Test
  void recoverableErrorsAreSanitizedAndSessionScoped() throws Exception {
    var roomId = UUID.randomUUID();
    var clientMessageId = UUID.randomUUID();
    var rawDetail = "user " + UUID.randomUUID() + " cannot access room " + roomId;
    var handler = new MessageStompExceptionHandler();
    var error =
        handler.handleRecoverable(
            new RecoverableMessageException(
                new MessageValidationException(rawDetail), roomId, clientMessageId));

    assertThat(error.errorCode()).isEqualTo("MESSAGE_VALIDATION_FAILED");
    assertThat(error.message()).isEqualTo("The message request is invalid.");
    assertThat(error.message()).doesNotContain(rawDetail);
    assertThat(error.roomId()).isEqualTo(roomId);
    assertThat(error.clientMessageId()).isEqualTo(clientMessageId);

    var annotation =
        MessageStompExceptionHandler.class
            .getDeclaredMethod("handleRecoverable", RecoverableMessageException.class)
            .getAnnotation(SendToUser.class);
    assertThat(annotation.destinations()).containsExactly("/queue/errors");
    assertThat(annotation.broadcast()).isFalse();
  }

  @Test
  void knownRecoverableFailuresUseStableCodesAndMessages() {
    var roomId = UUID.randomUUID();
    var handler = new MessageStompExceptionHandler();

    var inaccessible =
        handler.handleRecoverable(
            new RecoverableMessageException(new MessageRoomAccessDeniedException(), roomId, null));
    var conflict =
        handler.handleRecoverable(
            new RecoverableMessageException(
                new MessageIdempotencyConflictException(), roomId, null));

    assertThat(inaccessible.errorCode()).isEqualTo("MESSAGE_ROOM_NOT_FOUND");
    assertThat(inaccessible.message()).isEqualTo("The room was not found or is not accessible.");
    assertThat(conflict.errorCode()).isEqualTo("MESSAGE_IDEMPOTENCY_CONFLICT");
    assertThat(conflict.message())
        .isEqualTo("The client message ID has already been used for another message.");
  }

  @Test
  void malformedPayloadErrorsAreSanitizedAndSessionScoped() throws Exception {
    var handler = new MessageStompExceptionHandler();

    var error = handler.handleMalformedPayload();

    assertThat(error.errorCode()).isEqualTo("MESSAGE_VALIDATION_FAILED");
    assertThat(error.message()).isEqualTo("The message request is invalid.");
    assertThat(error.roomId()).isNull();
    assertThat(error.clientMessageId()).isNull();
    var annotation =
        MessageStompExceptionHandler.class
            .getDeclaredMethod("handleMalformedPayload")
            .getAnnotation(SendToUser.class);
    assertThat(annotation.destinations()).containsExactly("/queue/errors");
    assertThat(annotation.broadcast()).isFalse();
  }

  private static Principal principal(UUID userId) {
    return userId::toString;
  }

  private static MessageView message(
      UUID roomId, UUID senderId, UUID clientMessageId, String text) {
    return new MessageView(
        UUID.randomUUID(),
        roomId,
        senderId,
        clientMessageId,
        text,
        Instant.parse("2026-08-13T10:00:00Z"));
  }
}
