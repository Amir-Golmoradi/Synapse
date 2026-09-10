package dev.amir.synapse.shared.websocket.api;

import java.util.UUID;
import org.springframework.messaging.simp.stomp.StompCommand;

/** Capability-owned authorization for client-selectable STOMP destinations. */
public interface StompDestinationPolicy {

  boolean supports(StompCommand command, String destination);

  void authorize(StompCommand command, String destination, UUID userId);
}
