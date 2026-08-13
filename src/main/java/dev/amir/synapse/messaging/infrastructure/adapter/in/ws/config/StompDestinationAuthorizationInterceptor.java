package dev.amir.synapse.messaging.infrastructure.adapter.in.ws.config;

import dev.amir.synapse.messaging.domain.port.out.LoadRoomPort;
import java.util.UUID;
import java.util.regex.Pattern;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

@Component
public class StompDestinationAuthorizationInterceptor implements ChannelInterceptor {
  private static final String UUID_PATTERN =
      "[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}";
  private static final Pattern MESSAGE_DESTINATION =
      Pattern.compile("^/app/rooms/(" + UUID_PATTERN + ")/messages$");
  private static final Pattern ROOM_TOPIC =
      Pattern.compile("^/topic/rooms/(" + UUID_PATTERN + ")$");
  private static final String PRIVATE_ERROR_DESTINATION = "/user/queue/errors";
  private static final String ACCESS_DENIED_MESSAGE = "STOMP request rejected";

  private final LoadRoomPort loadRoomPort;
  private final StompClientErrorSender errorSender;

  public StompDestinationAuthorizationInterceptor(
      LoadRoomPort loadRoomPort, StompClientErrorSender errorSender) {
    this.loadRoomPort = loadRoomPort;
    this.errorSender = errorSender;
  }

  @Override
  public @Nullable Message<?> preSend(
      @NonNull Message<?> message, @NonNull MessageChannel channel) {
    var accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
    if (accessor == null) {
      return message;
    }

    try {
      authorizeCommand(accessor);
      return message;
    } catch (AccessDeniedException exception) {
      errorSender.reject(accessor);
      return null;
    }
  }

  private void authorizeCommand(StompHeaderAccessor accessor) {
    var command = accessor.getCommand();
    if (command == null) {
      return;
    }

    switch (command) {
      case SEND -> authorizeSend(accessor);
      case SUBSCRIBE -> authorizeSubscription(accessor);
      case CONNECT, STOMP, UNSUBSCRIBE, DISCONNECT -> {
        // Authentication runs first; these commands have no client-selectable broker destination.
      }
      default -> throw accessDenied();
    }
  }

  private void authorizeSend(StompHeaderAccessor accessor) {
    requireAuthenticatedUserId(accessor);
    var destination = accessor.getDestination();
    if (destination == null || !MESSAGE_DESTINATION.matcher(destination).matches()) {
      throw accessDenied();
    }
  }

  private void authorizeSubscription(StompHeaderAccessor accessor) {
    var userId = requireAuthenticatedUserId(accessor);
    var destination = accessor.getDestination();
    if (PRIVATE_ERROR_DESTINATION.equals(destination)) {
      return;
    }

    var matcher = destination == null ? null : ROOM_TOPIC.matcher(destination);
    if (matcher == null || !matcher.matches()) {
      throw accessDenied();
    }

    var roomId = UUID.fromString(matcher.group(1));
    if (!loadRoomPort.hasActiveMembership(roomId, userId)) {
      throw accessDenied();
    }
  }

  private static UUID requireAuthenticatedUserId(StompHeaderAccessor accessor) {
    if (!(accessor.getUser() instanceof Authentication authentication)
        || !authentication.isAuthenticated()) {
      throw accessDenied();
    }

    try {
      return UUID.fromString(authentication.getName());
    } catch (IllegalArgumentException exception) {
      throw accessDenied(exception);
    }
  }

  private static AccessDeniedException accessDenied() {
    return new AccessDeniedException(ACCESS_DENIED_MESSAGE);
  }

  private static AccessDeniedException accessDenied(Throwable cause) {
    return new AccessDeniedException(ACCESS_DENIED_MESSAGE, cause);
  }
}
