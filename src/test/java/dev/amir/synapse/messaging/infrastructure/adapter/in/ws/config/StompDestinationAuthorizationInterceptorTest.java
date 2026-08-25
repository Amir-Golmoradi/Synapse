package dev.amir.synapse.messaging.infrastructure.adapter.in.ws.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import dev.amir.synapse.messaging.domain.port.out.LoadRoomPort;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

class StompDestinationAuthorizationInterceptorTest {

  private final LoadRoomPort loadRoomPort = mock(LoadRoomPort.class);
  private final StompClientErrorSender errorSender = mock(StompClientErrorSender.class);
  private final MessageChannel channel = mock(MessageChannel.class);
  private final StompDestinationAuthorizationInterceptor interceptor =
      new StompDestinationAuthorizationInterceptor(loadRoomPort, errorSender);

  @Test
  void allowsOnlyTheApplicationRoomMessageDestinationForSend() {
    var userId = UUID.randomUUID();
    var roomId = UUID.randomUUID();
    var message = message(StompCommand.SEND, "/app/rooms/" + roomId + "/messages", userId);

    assertThat(interceptor.preSend(message, channel)).isSameAs(message);
    verifyNoInteractions(loadRoomPort);
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "/topic/rooms/00000000-0000-0000-0000-000000000001",
        "/queue/errors",
        "/user/queue/errors",
        "/app/rooms/not-a-uuid/messages",
        "/app/rooms/00000000-0000-0000-0000-000000000001/messages/extra"
      })
  void rejectsAllOtherSendDestinations(String destination) {
    var message = message(StompCommand.SEND, destination, UUID.randomUUID());

    assertRejected(message);
    verifyNoInteractions(loadRoomPort);
  }

  @Test
  void allowsActiveMembersToSubscribeToTheirRoomTopic() {
    var userId = UUID.randomUUID();
    var roomId = UUID.randomUUID();
    var message = message(StompCommand.SUBSCRIBE, "/topic/rooms/" + roomId, userId);
    when(loadRoomPort.hasActiveMembership(roomId, userId)).thenReturn(true);

    assertThat(interceptor.preSend(message, channel)).isSameAs(message);
    verify(loadRoomPort).hasActiveMembership(roomId, userId);
  }

  @Test
  void rejectsInactiveOrMissingRoomMembershipWithoutExposingTheReason() {
    var userId = UUID.randomUUID();
    var roomId = UUID.randomUUID();
    var message = message(StompCommand.SUBSCRIBE, "/topic/rooms/" + roomId, userId);
    when(loadRoomPort.hasActiveMembership(roomId, userId)).thenReturn(false);

    assertRejected(message);
  }

  @Test
  void allowsAnAuthenticatedUserToSubscribeToTheirPrivateErrorQueue() {
    var message = message(StompCommand.SUBSCRIBE, "/user/queue/errors", UUID.randomUUID());

    assertThat(interceptor.preSend(message, channel)).isSameAs(message);
    verifyNoInteractions(loadRoomPort);
  }

  @Test
  void rejectsUnauthenticatedSendAndSubscribeFrames() {
    assertRejected(message(StompCommand.SEND, validMessageDestination(), null));
    assertRejected(message(StompCommand.SUBSCRIBE, "/user/queue/errors", null));
    verifyNoInteractions(loadRoomPort);
  }

  @Test
  void leavesLifecycleFramesToTheStompProtocolHandler() {
    var disconnect = message(StompCommand.DISCONNECT, null, null);
    var unsubscribe = message(StompCommand.UNSUBSCRIBE, null, null);

    assertThat(interceptor.preSend(disconnect, channel)).isSameAs(disconnect);
    assertThat(interceptor.preSend(unsubscribe, channel)).isSameAs(unsubscribe);
    verifyNoInteractions(loadRoomPort);
  }

  @ParameterizedTest
  @ValueSource(strings = {"MESSAGE", "CONNECTED", "ERROR", "BEGIN", "COMMIT", "ABORT"})
  void rejectsServerOnlyAndUnsupportedTransactionCommands(String commandName) {
    var message =
        message(
            StompCommand.valueOf(commandName),
            "/topic/rooms/" + UUID.randomUUID(),
            UUID.randomUUID());

    assertRejected(message);
    verifyNoInteractions(loadRoomPort);
  }

  private void assertRejected(Message<byte[]> message) {
    assertThat(interceptor.preSend(message, channel)).isNull();
    verify(errorSender)
        .reject(
            org.springframework.messaging.support.MessageHeaderAccessor.getAccessor(
                message, StompHeaderAccessor.class));
  }

  private static String validMessageDestination() {
    return "/app/rooms/" + UUID.randomUUID() + "/messages";
  }

  private static Message<byte[]> message(StompCommand command, String destination, UUID userId) {
    var accessor = StompHeaderAccessor.create(command);
    if (destination != null) {
      accessor.setDestination(destination);
    }
    if (userId != null) {
      accessor.setUser(
          UsernamePasswordAuthenticationToken.authenticated(userId.toString(), null, List.of()));
    }
    accessor.setLeaveMutable(true);
    return MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
  }
}
