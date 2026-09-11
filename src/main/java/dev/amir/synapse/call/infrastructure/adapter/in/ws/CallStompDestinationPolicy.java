package dev.amir.synapse.call.infrastructure.adapter.in.ws;

import dev.amir.synapse.shared.websocket.api.StompDestinationPolicy;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.stereotype.Component;

@Component
public class CallStompDestinationPolicy implements StompDestinationPolicy {
  private static final String UUID_PATTERN =
      "[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}";
  private static final Pattern CALL_SEND =
      Pattern.compile("^/app/calls/" + UUID_PATTERN + "/(signals|control)$");
  private static final Set<String> SEND_DESTINATIONS = Set.of("/app/calls/client-ready");
  private static final Set<String> SUBSCRIBE_DESTINATIONS =
      Set.of("/user/queue/calls/events", "/user/queue/calls/signals", "/user/queue/calls/errors");

  @Override
  public boolean supports(StompCommand command, String destination) {
    return command == StompCommand.SEND
        ? SEND_DESTINATIONS.contains(destination) || CALL_SEND.matcher(destination).matches()
        : command == StompCommand.SUBSCRIBE && SUBSCRIBE_DESTINATIONS.contains(destination);
  }

  @Override
  public void authorize(StompCommand command, String destination, UUID userId) {
    // Authentication and the exact destination allowlist are sufficient at this transport layer.
    // Call participation and media ownership are checked transactionally by the application layer.
  }
}
