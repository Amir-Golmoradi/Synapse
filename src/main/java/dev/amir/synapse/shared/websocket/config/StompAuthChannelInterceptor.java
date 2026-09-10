package dev.amir.synapse.shared.websocket.config;

import dev.amir.synapse.shared.websocket.api.StompAuthenticator;
import java.util.List;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.springframework.http.HttpHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Component;

@Component
public class StompAuthChannelInterceptor implements ChannelInterceptor {
  private static final String BEARER_PREFIX = "Bearer ";
  private final StompAuthenticator authenticator;
  private final StompClientErrorSender errorSender;

  public StompAuthChannelInterceptor(
      StompAuthenticator authenticator, StompClientErrorSender errorSender) {
    this.authenticator = authenticator;
    this.errorSender = errorSender;
  }

  @Override
  public @Nullable Message<?> preSend(
      @NonNull Message<?> message, @NonNull MessageChannel channel) {
    var accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
    if (accessor == null) {
      return message;
    }
    var authorization = accessor.getFirstNativeHeader(HttpHeaders.AUTHORIZATION);
    if (authorization != null && accessor.isMutable()) {
      accessor.removeNativeHeader(HttpHeaders.AUTHORIZATION);
    }
    if (!StompCommand.CONNECT.equals(accessor.getCommand())
        && !StompCommand.STOMP.equals(accessor.getCommand())) {
      return message;
    }
    try {
      var token = extractAccessToken(authorization);
      var userId = authenticator.authenticate(token).orElseThrow(StompAuthChannelInterceptor::bad);
      accessor.setUser(
          UsernamePasswordAuthenticationToken.authenticated(userId.toString(), null, List.of()));
      return message;
    } catch (RuntimeException exception) {
      errorSender.reject(accessor);
      return null;
    }
  }

  private static String extractAccessToken(String authorization) {
    if (authorization == null || !authorization.startsWith(BEARER_PREFIX)) {
      throw bad();
    }
    var token = authorization.substring(BEARER_PREFIX.length());
    if (token.isBlank() || !token.equals(token.strip())) {
      throw bad();
    }
    return token;
  }

  private static BadCredentialsException bad() {
    return new BadCredentialsException("STOMP authentication failed");
  }
}
