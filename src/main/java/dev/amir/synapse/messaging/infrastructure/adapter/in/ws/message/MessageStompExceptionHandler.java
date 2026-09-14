package dev.amir.synapse.messaging.infrastructure.adapter.in.ws.message;

import dev.amir.synapse.messaging.domain.exception.MessageIdempotencyConflictException;
import dev.amir.synapse.messaging.domain.exception.MessageRoomAccessDeniedException;
import dev.amir.synapse.messaging.domain.exception.MessageValidationException;
import dev.amir.synapse.shared.domain.DomainException;
import org.springframework.messaging.converter.MessageConversionException;
import org.springframework.messaging.handler.annotation.MessageExceptionHandler;
import org.springframework.messaging.handler.invocation.MethodArgumentResolutionException;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.web.bind.annotation.ControllerAdvice;

@ControllerAdvice(assignableTypes = MessageStompController.class)
public class MessageStompExceptionHandler {

  @MessageExceptionHandler(RecoverableMessageException.class)
  @SendToUser(destinations = "/queue/errors", broadcast = false)
  MessageErrorResponse handleRecoverable(RecoverableMessageException exception) {
    var domainException = exception.getDomainException();
    return new MessageErrorResponse(
        domainException.getErrorCode(),
        sanitizedMessage(domainException),
        exception.getRoomId(),
        exception.getClientMessageId());
  }

  @MessageExceptionHandler({
    MessageConversionException.class,
    MethodArgumentResolutionException.class
  })
  @SendToUser(destinations = "/queue/errors", broadcast = false)
  MessageErrorResponse handleMalformedPayload() {
    return new MessageErrorResponse(
        "MESSAGE_VALIDATION_FAILED", "The message request is invalid.", null, null);
  }

  private static String sanitizedMessage(DomainException exception) {
    if (exception instanceof MessageValidationException) {
      return "The message request is invalid.";
    }
    if (exception instanceof MessageRoomAccessDeniedException) {
      return "The room was not found or is not accessible.";
    }
    if (exception instanceof MessageIdempotencyConflictException) {
      return "The client message ID has already been used for another message.";
    }
    return "The message could not be sent.";
  }
}
