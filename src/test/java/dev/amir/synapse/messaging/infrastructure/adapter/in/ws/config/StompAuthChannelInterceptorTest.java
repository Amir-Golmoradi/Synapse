package dev.amir.synapse.messaging.infrastructure.adapter.in.ws.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import dev.amir.synapse.identity.domain.port.in.access_token.AuthenticateAccessTokenQuery;
import dev.amir.synapse.identity.domain.port.in.access_token.AuthenticateAccessTokenUseCase;
import dev.amir.synapse.identity.domain.value_object.UserId;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.http.HttpHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.messaging.support.MessageHeaderAccessor;

class StompAuthChannelInterceptorTest {

  private final AuthenticateAccessTokenUseCase authenticateAccessToken =
      mock(AuthenticateAccessTokenUseCase.class);
  private final StompClientErrorSender errorSender = mock(StompClientErrorSender.class);
  private final MessageChannel channel = mock(MessageChannel.class);
  private final StompAuthChannelInterceptor interceptor =
      new StompAuthChannelInterceptor(authenticateAccessToken, errorSender);

  @ParameterizedTest
  @EnumSource(
      value = StompCommand.class,
      names = {"CONNECT", "STOMP"})
  void authenticatesConnectFramesOnTheOriginalAccessor(StompCommand command) {
    var userId = UUID.randomUUID();
    var accessor = accessor(command);
    accessor.setNativeHeader(HttpHeaders.AUTHORIZATION, "Bearer access-token");
    var message = message(accessor);
    when(authenticateAccessToken.handle(new AuthenticateAccessTokenQuery("access-token")))
        .thenReturn(Optional.of(new UserId(userId)));

    var result = interceptor.preSend(message, channel);

    assertThat(result).isSameAs(message);
    assertThat(MessageHeaderAccessor.getAccessor(result, StompHeaderAccessor.class))
        .isSameAs(accessor);
    assertThat(accessor.getUser()).isNotNull();
    assertThat(accessor.getUser().getName()).isEqualTo(userId.toString());
    assertThat(accessor.getFirstNativeHeader(HttpHeaders.AUTHORIZATION)).isNull();
    verify(authenticateAccessToken).handle(new AuthenticateAccessTokenQuery("access-token"));
  }

  @Test
  void rejectsMissingAuthorizationWithASanitizedFailure() {
    var accessor = accessor(StompCommand.CONNECT);
    var message = message(accessor);

    assertThat(interceptor.preSend(message, channel)).isNull();
    verify(errorSender).reject(accessor);
    verifyNoInteractions(authenticateAccessToken);
  }

  @Test
  void rejectsInvalidTokenAndRemovesItFromTheFrame() {
    var accessor = accessor(StompCommand.CONNECT);
    accessor.setNativeHeader(HttpHeaders.AUTHORIZATION, "Bearer sensitive-token");
    var message = message(accessor);
    when(authenticateAccessToken.handle(new AuthenticateAccessTokenQuery("sensitive-token")))
        .thenReturn(Optional.empty());

    assertThat(interceptor.preSend(message, channel)).isNull();
    assertThat(accessor.getFirstNativeHeader(HttpHeaders.AUTHORIZATION)).isNull();
    verify(errorSender).reject(accessor);
  }

  @Test
  void stripsAuthorizationFromNonConnectFramesWithoutReauthenticating() {
    var accessor = accessor(StompCommand.SEND);
    accessor.setNativeHeader(HttpHeaders.AUTHORIZATION, "Bearer should-not-reach-handler");
    var message = message(accessor);

    assertThat(interceptor.preSend(message, channel)).isSameAs(message);
    assertThat(accessor.getFirstNativeHeader(HttpHeaders.AUTHORIZATION)).isNull();
    verifyNoInteractions(authenticateAccessToken);
  }

  @Test
  void doesNotMutateAnImmutableSyntheticDisconnect() {
    var accessor = accessor(StompCommand.DISCONNECT);
    var message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

    assertThat(interceptor.preSend(message, channel)).isSameAs(message);
    verifyNoInteractions(authenticateAccessToken, errorSender);
  }

  private static StompHeaderAccessor accessor(StompCommand command) {
    return StompHeaderAccessor.create(command);
  }

  private static Message<byte[]> message(StompHeaderAccessor accessor) {
    accessor.setLeaveMutable(true);
    return MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
  }
}
