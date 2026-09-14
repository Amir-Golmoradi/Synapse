package dev.amir.synapse.call.infrastructure.adapter.in.ws;

import dev.amir.synapse.call.application.service.CallConnectionService;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

@Component
public class CallSessionEventListener {
  private final CallConnectionService connections;

  public CallSessionEventListener(CallConnectionService connections) {
    this.connections = connections;
  }

  @EventListener
  public void onDisconnect(SessionDisconnectEvent event) {
    connections.disconnect(event.getSessionId());
  }
}
