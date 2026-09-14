package dev.amir.synapse.call.infrastructure.adapter.out.ws;

import dev.amir.synapse.call.application.model.CallSignal;
import dev.amir.synapse.call.application.port.out.CallSignalDeliveryPort;
import java.util.Map;
import java.util.UUID;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

@Component
public class StompCallDeliveryAdapter implements CallSignalDeliveryPort {
  private final SimpMessagingTemplate messagingTemplate;

  public StompCallDeliveryAdapter(SimpMessagingTemplate messagingTemplate) {
    this.messagingTemplate = messagingTemplate;
  }

  @Override
  public void sendSignal(
      UUID userId, String sessionId, UUID callId, UUID senderId, CallSignal signal) {
    sendToSession(
        userId,
        sessionId,
        "/queue/calls/signals",
        Map.of("callId", callId, "senderId", senderId, "signal", signal));
  }

  @Override
  public void sendInstruction(UUID userId, String sessionId, Object instruction) {
    sendToSession(userId, sessionId, "/queue/calls/events", instruction);
  }

  private void sendToSession(UUID userId, String sessionId, String destination, Object payload) {
    var headers = SimpMessageHeaderAccessor.create();
    headers.setSessionId(sessionId);
    headers.setLeaveMutable(true);
    messagingTemplate.convertAndSendToUser(
        userId.toString(), destination, payload, headers.getMessageHeaders());
  }
}
