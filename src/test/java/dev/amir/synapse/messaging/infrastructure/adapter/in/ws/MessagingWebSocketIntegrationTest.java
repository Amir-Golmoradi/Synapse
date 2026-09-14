package dev.amir.synapse.messaging.infrastructure.adapter.in.ws;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;

import dev.amir.synapse.identity.application.port.out.access_token.CreateAccessTokenPort;
import dev.amir.synapse.identity.domain.value_object.UserId;
import dev.amir.synapse.messaging.domain.model.Room;
import dev.amir.synapse.messaging.domain.port.in.message.MessageView;
import dev.amir.synapse.messaging.domain.port.out.SaveRoomPort;
import dev.amir.synapse.messaging.domain.value_object.MemberId;
import dev.amir.synapse.messaging.infrastructure.adapter.in.ws.message.MessageErrorResponse;
import dev.amir.synapse.messaging.infrastructure.adapter.in.ws.message.SendMessageRequest;
import java.lang.reflect.Type;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpHeaders;
import org.springframework.messaging.converter.JacksonJsonMessageConverter;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompFrameHandler;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.web.socket.WebSocketHttpHeaders;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = {
      "spring.jpa.hibernate.ddl-auto=validate",
      "spring.flyway.enabled=true",
      "spring.flyway.locations=classpath:db/create-table,classpath:db/alter-table",
      "spring.security.oauth2.client.registration.google.client-id=test-google-client-id",
      "spring.security.oauth2.client.registration.google.client-secret=test-google-client-secret",
      "synapse.google-token-url=http://localhost/tokeninfo?id_token={idToken}",
      "synapse.jwt.secret=01234567890123456789012345678901",
      "synapse.jwt.token-expiration-ms=900000",
      "synapse.websocket.allowed-origins="
    })
@Testcontainers
class MessagingWebSocketIntegrationTest {

  @Container
  private static final PostgreSQLContainer<?> POSTGRES =
      new PostgreSQLContainer<>("postgres:16-alpine")
          .withDatabaseName("synapse_websocket_test")
          .withUsername("synapse")
          .withPassword("synapse");

  @LocalServerPort private int port;

  @Autowired private SaveRoomPort saveRoomPort;

  @Autowired private CreateAccessTokenPort createAccessTokenPort;

  @DynamicPropertySource
  static void registerDatasourceProperties(DynamicPropertyRegistry registry) {
    registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
    registry.add("spring.datasource.username", POSTGRES::getUsername);
    registry.add("spring.datasource.password", POSTGRES::getPassword);
  }

  @Test
  void authenticatedMemberSendsAndReceivesACanonicalMessageOverNativeStomp() throws Exception {
    var senderId = UUID.randomUUID();
    var room = Room.createGroupRoom(MemberId.of(senderId), "Realtime", null);
    saveRoomPort.save(room);
    var roomId = room.getId().getValue();
    var clientMessageId = UUID.randomUUID();
    var received = new CompletableFuture<MessageView>();
    var client = stompClient();
    StompSession session = null;

    try {
      session =
          client
              .connectAsync(
                  webSocketUrl(),
                  new WebSocketHttpHeaders(),
                  connectHeaders(accessToken(senderId)),
                  sessionHandler())
              .get(10, SECONDS);
      session.subscribe("/topic/rooms/" + roomId, frameHandler(MessageView.class, received));

      var sendHeaders = new StompHeaders();
      sendHeaders.setDestination("/app/rooms/" + roomId + "/messages");
      sendHeaders.setContentType(org.springframework.http.MediaType.APPLICATION_JSON);
      session.send(sendHeaders, new SendMessageRequest(clientMessageId, "hello over STOMP"));

      assertThat(received.get(10, SECONDS))
          .satisfies(
              message -> {
                assertThat(message.messageId()).isNotNull();
                assertThat(message.roomId()).isEqualTo(roomId);
                assertThat(message.senderId()).isEqualTo(senderId);
                assertThat(message.clientMessageId()).isEqualTo(clientMessageId);
                assertThat(message.text()).isEqualTo("hello over STOMP");
                assertThat(message.createdAt()).isNotNull();
              });
    } finally {
      if (session != null && session.isConnected()) {
        session.disconnect();
      }
      client.stop();
    }
  }

  @Test
  void handshakeIsPublicButAConnectWithoutABearerTokenGetsASanitizedError() throws Exception {
    var errorFrame = new CompletableFuture<StompHeaders>();
    var client = stompClient();

    try {
      client.connectAsync(
          webSocketUrl(), new WebSocketHttpHeaders(), new StompHeaders(), errorHandler(errorFrame));

      assertThat(errorFrame.get(10, SECONDS).getFirst(StompHeaderAccessor.STOMP_MESSAGE_HEADER))
          .isEqualTo("STOMP request rejected");
    } finally {
      client.stop();
    }
  }

  @Test
  void malformedMessagePayloadReturnsAPrivateSanitizedValidationError() throws Exception {
    var senderId = UUID.randomUUID();
    var room = Room.createGroupRoom(MemberId.of(senderId), "Malformed", null);
    saveRoomPort.save(room);
    var error = new CompletableFuture<MessageErrorResponse>();
    var client = stompClient();
    StompSession session = null;

    try {
      session =
          client
              .connectAsync(
                  webSocketUrl(),
                  new WebSocketHttpHeaders(),
                  connectHeaders(accessToken(senderId)),
                  sessionHandler())
              .get(10, SECONDS);
      session.subscribe("/user/queue/errors", frameHandler(MessageErrorResponse.class, error));

      var sendHeaders = new StompHeaders();
      sendHeaders.setDestination("/app/rooms/" + room.getId().getValue() + "/messages");
      sendHeaders.setContentType(org.springframework.http.MediaType.APPLICATION_JSON);
      session.send(sendHeaders, "not a message request");

      assertThat(error.get(10, SECONDS))
          .satisfies(
              response -> {
                assertThat(response.errorCode()).isEqualTo("MESSAGE_VALIDATION_FAILED");
                assertThat(response.message()).isEqualTo("The message request is invalid.");
                assertThat(response.roomId()).isNull();
                assertThat(response.clientMessageId()).isNull();
              });
    } finally {
      if (session != null && session.isConnected()) {
        session.disconnect();
      }
      client.stop();
    }
  }

  private WebSocketStompClient stompClient() {
    var client = new WebSocketStompClient(new StandardWebSocketClient());
    client.setMessageConverter(new JacksonJsonMessageConverter());
    client.start();
    return client;
  }

  private String webSocketUrl() {
    return "ws://localhost:" + port + "/ws";
  }

  private String accessToken(UUID userId) {
    return createAccessTokenPort.createAccessToken(new UserId(userId));
  }

  private static StompHeaders connectHeaders(String accessToken) {
    var headers = new StompHeaders();
    headers.set(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken);
    return headers;
  }

  private static StompSessionHandlerAdapter sessionHandler() {
    return new StompSessionHandlerAdapter() {};
  }

  private static <T> StompFrameHandler frameHandler(
      Class<T> payloadType, CompletableFuture<T> received) {
    return new StompFrameHandler() {
      @Override
      public Type getPayloadType(StompHeaders headers) {
        return payloadType;
      }

      @Override
      public void handleFrame(StompHeaders headers, Object payload) {
        received.complete(payloadType.cast(payload));
      }
    };
  }

  private static StompSessionHandlerAdapter errorHandler(
      CompletableFuture<StompHeaders> errorFrame) {
    return new StompSessionHandlerAdapter() {
      @Override
      public void handleFrame(StompHeaders headers, Object payload) {
        errorFrame.complete(headers);
      }

      @Override
      public void handleException(
          StompSession session,
          StompCommand command,
          StompHeaders headers,
          byte[] payload,
          Throwable exception) {
        errorFrame.completeExceptionally(exception);
      }

      @Override
      public void handleTransportError(StompSession session, Throwable exception) {
        if (!errorFrame.isDone()) {
          errorFrame.completeExceptionally(exception);
        }
      }
    };
  }
}
