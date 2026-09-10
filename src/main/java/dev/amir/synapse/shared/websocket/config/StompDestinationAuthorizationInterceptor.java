package dev.amir.synapse.shared.websocket.config;

import dev.amir.synapse.shared.websocket.api.StompDestinationPolicy;
import java.util.List;
import java.util.UUID;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

@Component
public class StompDestinationAuthorizationInterceptor implements ChannelInterceptor {
  private static final String PRIVATE_ERROR_DESTINATION = "/user/queue/errors";
  private final List<StompDestinationPolicy> policies;
  private final StompClientErrorSender errorSender;

  public StompDestinationAuthorizationInterceptor(
      List<StompDestinationPolicy> policies, StompClientErrorSender errorSender) {
    this.policies = List.copyOf(policies);
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
      authorize(accessor);
      return message;
    } catch (AccessDeniedException exception) {
      errorSender.reject(accessor);
      return null;
    }
  }

  private void authorize(StompHeaderAccessor accessor) {
    var command = accessor.getCommand();
    if (command == null) {
      return;
    }
    if (command == StompCommand.CONNECT
        || command == StompCommand.STOMP
        || command == StompCommand.UNSUBSCRIBE
        || command == StompCommand.DISCONNECT) {
      return;
    }
    if (command != StompCommand.SEND && command != StompCommand.SUBSCRIBE) {
      throw denied();
    }
    var userId = requireUser(accessor);
    var destination = accessor.getDestination();
    if (command == StompCommand.SUBSCRIBE && PRIVATE_ERROR_DESTINATION.equals(destination)) {
      return;
    }
    if (destination == null) {
      throw denied();
    }
    var matching = policies.stream().filter(p -> p.supports(command, destination)).toList();
    if (matching.size() != 1) {
      throw denied();
    }
    matching.getFirst().authorize(command, destination, userId);
  }

  private static UUID requireUser(StompHeaderAccessor accessor) {
    if (!(accessor.getUser() instanceof Authentication authentication)
        || !authentication.isAuthenticated()) {
      throw denied();
    }
    try {
      return UUID.fromString(authentication.getName());
    } catch (IllegalArgumentException exception) {
      throw new AccessDeniedException("STOMP request rejected", exception);
    }
  }

  private static AccessDeniedException denied() {
    return new AccessDeniedException("STOMP request rejected");
  }
}
