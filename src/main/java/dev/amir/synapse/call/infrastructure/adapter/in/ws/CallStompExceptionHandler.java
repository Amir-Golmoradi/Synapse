package dev.amir.synapse.call.infrastructure.adapter.in.ws;

import org.springframework.messaging.converter.MessageConversionException;
import org.springframework.messaging.handler.annotation.MessageExceptionHandler;
import org.springframework.messaging.handler.invocation.MethodArgumentResolutionException;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.web.bind.annotation.ControllerAdvice;

@ControllerAdvice(assignableTypes = CallStompController.class)
public class CallStompExceptionHandler {
  @MessageExceptionHandler(RecoverableCallStompException.class)
  @SendToUser(destinations = "/queue/calls/errors", broadcast = false)
  CallStompErrorResponse handle(RecoverableCallStompException exception) {
    var domain = exception.domainException();
    return new CallStompErrorResponse(
        domain.getErrorCode(), "The call operation could not be completed.", exception.callId());
  }

  @MessageExceptionHandler({
    MessageConversionException.class,
    MethodArgumentResolutionException.class
  })
  @SendToUser(destinations = "/queue/calls/errors", broadcast = false)
  CallStompErrorResponse malformed() {
    return new CallStompErrorResponse(
        "CALL_VALIDATION_FAILED", "The call request is invalid.", null);
  }
}
