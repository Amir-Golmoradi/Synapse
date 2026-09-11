package dev.amir.synapse.messaging.infrastructure.adapter.in.ws.config;

import dev.amir.synapse.messaging.domain.port.out.LoadRoomPort;
import dev.amir.synapse.shared.websocket.api.StompDestinationPolicy;
import java.util.UUID;
import java.util.regex.Pattern;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

@Component
public class MessagingStompDestinationPolicy implements StompDestinationPolicy {
  private static final String UUID_PATTERN =
      "[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}";
  private static final Pattern MESSAGE_DESTINATION =
      Pattern.compile("^/app/rooms/(" + UUID_PATTERN + ")/messages$");
  private static final Pattern ROOM_TOPIC =
      Pattern.compile("^/topic/rooms/(" + UUID_PATTERN + ")$");
  private final LoadRoomPort loadRoomPort;

  public MessagingStompDestinationPolicy(LoadRoomPort loadRoomPort) {
    this.loadRoomPort = loadRoomPort;
  }

  @Override
  public boolean supports(StompCommand command, String destination) {
    return (command == StompCommand.SEND && MESSAGE_DESTINATION.matcher(destination).matches())
        || (command == StompCommand.SUBSCRIBE && ROOM_TOPIC.matcher(destination).matches());
  }

  @Override
  public void authorize(StompCommand command, String destination, UUID userId) {
    if (command == StompCommand.SUBSCRIBE) {
      var matcher = ROOM_TOPIC.matcher(destination);
      if (!matcher.matches()
          || !loadRoomPort.hasActiveMembership(UUID.fromString(matcher.group(1)), userId)) {
        throw new AccessDeniedException("STOMP request rejected");
      }
    }
  }
}
