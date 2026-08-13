package dev.amir.synapse.messaging.infrastructure.adapter.in.ws.config;

import dev.amir.synapse.identity.domain.port.in.access_token.AuthenticateAccessTokenQuery;
import dev.amir.synapse.identity.domain.port.in.access_token.AuthenticateAccessTokenUseCase;
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
  private static final String AUTHENTICATION_FAILURE_MESSAGE = "STOMP authentication failed";

  private final AuthenticateAccessTokenUseCase authenticateAccessToken;
  private final StompClientErrorSender errorSender;

  public StompAuthChannelInterceptor(
      AuthenticateAccessTokenUseCase authenticateAccessToken, StompClientErrorSender errorSender) {
    this.authenticateAccessToken = authenticateAccessToken;
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

    if (!isConnect(accessor.getCommand())) {
      return message;
    }

    try {
      var accessToken = extractAccessToken(authorization);
      var userId =
          authenticateAccessToken
              .handle(new AuthenticateAccessTokenQuery(accessToken))
              .orElseThrow(StompAuthChannelInterceptor::authenticationFailure);
      var authentication =
          UsernamePasswordAuthenticationToken.authenticated(
              userId.getValue().toString(), null, List.of());
      accessor.setUser(authentication);
      return message;
    } catch (RuntimeException exception) {
      errorSender.reject(accessor);
      return null;
    }
  }

  private static boolean isConnect(StompCommand command) {
    return StompCommand.CONNECT.equals(command) || StompCommand.STOMP.equals(command);
  }

  private static String extractAccessToken(String authorization) {
    if (authorization == null || !authorization.startsWith(BEARER_PREFIX)) {
      throw authenticationFailure();
    }

    var accessToken = authorization.substring(BEARER_PREFIX.length());
    if (accessToken.isBlank() || !accessToken.equals(accessToken.strip())) {
      throw authenticationFailure();
    }
    return accessToken;
  }

  private static BadCredentialsException authenticationFailure() {
    return new BadCredentialsException(AUTHENTICATION_FAILURE_MESSAGE);
  }
}
