package dev.amir.synapse.call.infrastructure.adapter.in.ws;

import dev.amir.synapse.call.application.service.CallConnectionService;
import dev.amir.synapse.call.application.service.CallSignalService;
import dev.amir.synapse.call.domain.exception.CallValidationException;
import dev.amir.synapse.call.domain.port.in.ResumeCallUseCase;
import dev.amir.synapse.shared.domain.DomainException;
import java.security.Principal;
import java.time.Clock;
import java.util.UUID;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.stereotype.Controller;

@Controller
public class CallStompController {
  private final CallConnectionService connections;
  private final CallSignalService signals;
  private final ResumeCallUseCase resume;
  private final Clock clock;

  public CallStompController(
      CallConnectionService connections,
      CallSignalService signals,
      ResumeCallUseCase resume,
      Clock callClock) {
    this.connections = connections;
    this.signals = signals;
    this.resume = resume;
    this.clock = callClock;
  }

  @MessageMapping("/calls/client-ready")
  @SendToUser(destinations = "/queue/calls/events", broadcast = false)
  public CallStompResponse register(
      @Payload ClientReadyRequest request,
      Principal principal,
      @Header("simpSessionId") String sessionId) {
    try {
      if (request == null || request.clientInstanceId() == null) {
        throw new CallValidationException("Client instance ID is required.");
      }
      connections.register(actor(principal), request.clientInstanceId(), sessionId);
      return new CallStompResponse(
          "CLIENT_READY", null, request.clientInstanceId(), null, clock.instant());
    } catch (DomainException exception) {
      throw new RecoverableCallStompException(exception, null);
    }
  }

  @MessageMapping("/calls/{callId}/signals")
  public void signal(
      @DestinationVariable UUID callId,
      @Payload RelaySignalRequest request,
      Principal principal,
      @Header("simpSessionId") String sessionId) {
    try {
      if (request == null || request.clientInstanceId() == null) {
        throw new CallValidationException("Signal request is invalid.");
      }
      signals.relay(
          callId, actor(principal), request.clientInstanceId(), sessionId, request.signal());
    } catch (DomainException exception) {
      throw new RecoverableCallStompException(exception, callId);
    }
  }

  @MessageMapping("/calls/{callId}/control")
  @SendToUser(destinations = "/queue/calls/events", broadcast = false)
  public CallStompResponse control(
      @DestinationVariable UUID callId,
      @Payload CallControlRequest request,
      Principal principal,
      @Header("simpSessionId") String sessionId) {
    try {
      validateControl(request);
      var actor = actor(principal);
      switch (request.type()) {
        case READY ->
            connections.ready(
                callId, actor, request.clientInstanceId(), request.generation(), sessionId);
        case CONNECTED ->
            connections.connected(
                callId, actor, request.clientInstanceId(), request.generation(), sessionId);
        case HEARTBEAT ->
            connections.heartbeat(
                callId, actor, request.clientInstanceId(), request.generation(), sessionId);
        case RECOVERY_REQUIRED ->
            resume.resume(
                new ResumeCallUseCase.Command(
                    callId,
                    actor,
                    request.clientInstanceId(),
                    request.requestId(),
                    request.generation(),
                    request.peerConnectionRetained()));
      }
      return new CallStompResponse(
          request.type().name(),
          callId,
          request.clientInstanceId(),
          request.generation(),
          clock.instant());
    } catch (DomainException exception) {
      throw new RecoverableCallStompException(exception, callId);
    }
  }

  private static void validateControl(CallControlRequest request) {
    if (request == null
        || request.type() == null
        || request.clientInstanceId() == null
        || request.generation() < 0
        || (request.type() == CallControlRequest.Type.RECOVERY_REQUIRED
            && request.requestId() == null)) {
      throw new CallValidationException("Call control request is invalid.");
    }
  }

  private static UUID actor(Principal principal) {
    return UUID.fromString(principal.getName());
  }
}
