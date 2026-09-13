package dev.amir.synapse.call.infrastructure.adapter.in.ws;

import io.micrometer.core.instrument.MeterRegistry;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.converter.MessageConversionException;
import org.springframework.messaging.handler.annotation.MessageExceptionHandler;
import org.springframework.messaging.handler.invocation.MethodArgumentResolutionException;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.web.bind.annotation.ControllerAdvice;

@ControllerAdvice(assignableTypes = CallStompController.class)
public class CallStompExceptionHandler {
  private static final Logger LOGGER = LoggerFactory.getLogger(CallStompExceptionHandler.class);
  private final Optional<MeterRegistry> meterRegistry;

  public CallStompExceptionHandler(Optional<MeterRegistry> meterRegistry) {
    this.meterRegistry = meterRegistry;
  }

  @MessageExceptionHandler(RecoverableCallStompException.class)
  @SendToUser(destinations = "/queue/calls/errors", broadcast = false)
  CallStompErrorResponse handle(RecoverableCallStompException exception) {
    var domain = exception.domainException();
    recordRejection(domain.getErrorCode(), exception.callId());
    return new CallStompErrorResponse(
        domain.getErrorCode(), "The call operation could not be completed.", exception.callId());
  }

  @MessageExceptionHandler({
    MessageConversionException.class,
    MethodArgumentResolutionException.class
  })
  @SendToUser(destinations = "/queue/calls/errors", broadcast = false)
  CallStompErrorResponse malformed() {
    recordRejection("CALL_VALIDATION_FAILED", null);
    return new CallStompErrorResponse(
        "CALL_VALIDATION_FAILED", "The call request is invalid.", null);
  }

  private void recordRejection(String errorCode, java.util.UUID callId) {
    meterRegistry.ifPresent(
        registry ->
            registry
                .counter("synapse.calls.stomp.rejections", "error_code", errorCode)
                .increment());
    LOGGER.warn("call_stomp_rejected callId={} errorCode={}", callId, errorCode);
  }
}
