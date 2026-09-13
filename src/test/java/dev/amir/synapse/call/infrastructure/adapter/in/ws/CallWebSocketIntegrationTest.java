package dev.amir.synapse.call.infrastructure.adapter.in.ws;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;

import dev.amir.synapse.call.application.model.CallRuntimeState;
import dev.amir.synapse.call.application.model.CallSignal;
import dev.amir.synapse.call.application.port.out.CallRuntimePort;
import dev.amir.synapse.call.domain.enums.CallMediaType;
import dev.amir.synapse.call.domain.model.Call;
import dev.amir.synapse.call.domain.port.out.SaveCallPort;
import dev.amir.synapse.call.domain.value_object.CallParticipants;
import dev.amir.synapse.identity.application.port.out.access_token.CreateAccessTokenPort;
import dev.amir.synapse.identity.domain.value_object.UserId;
import java.lang.reflect.Type;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpHeaders;
import org.springframework.messaging.converter.JacksonJsonMessageConverter;
import org.springframework.messaging.simp.stomp.StompFrameHandler;
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
      "synapse.websocket.allowed-origins=",
      "synapse.call.timeout-sweep=1h"
    })
@Testcontainers
class CallWebSocketIntegrationTest {
  @Container
  private static final PostgreSQLContainer<?> POSTGRES =
      new PostgreSQLContainer<>("postgres:16-alpine")
          .withDatabaseName("synapse_call_websocket_test")
          .withUsername("synapse")
          .withPassword("synapse");

  @LocalServerPort private int port;
  @Autowired private SaveCallPort saveCalls;
  @Autowired private CallRuntimePort runtime;
  @Autowired private CreateAccessTokenPort accessTokens;

  @DynamicPropertySource
  static void datasource(DynamicPropertyRegistry registry) {
    registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
    registry.add("spring.datasource.username", POSTGRES::getUsername);
    registry.add("spring.datasource.password", POSTGRES::getPassword);
  }

  @Test
  void privatelyRelaysVideoOfferToTheOwningCalleeSession() throws Exception {
    var caller = UUID.randomUUID();
    var callee = UUID.randomUUID();
    var callerInstance = UUID.randomUUID();
    var calleeInstance = UUID.randomUUID();
    var call = connectingVideoCall(caller, callee);
    var callId = call.getId().value();
    saveCalls.saveAndFlush(call);
    var lease = Instant.now().plusSeconds(60);
    runtime.save(
        new CallRuntimeState(
            callId,
            callerInstance,
            calleeInstance,
            1,
            true,
            true,
            false,
            false,
            lease,
            lease,
            null,
            null,
            null,
            null,
            null,
            null,
            0,
            0,
            "test-server"));

    var client = stompClient();
    StompSession callerSession = null;
    StompSession calleeSession = null;
    try {
      callerSession = connect(client, caller);
      calleeSession = connect(client, callee);
      register(callerSession, callerInstance);
      var received = new CompletableFuture<SignalEnvelope>();
      calleeSession.subscribe(
          "/user/queue/calls/signals", frameHandler(SignalEnvelope.class, received));
      register(calleeSession, calleeInstance);
      var offer =
          new CallSignal(
              UUID.randomUUID(),
              1,
              CallSignal.Type.OFFER,
              "v=0\r\nm=audio 9 UDP/TLS/RTP/SAVPF 111\r\nm=video 9 UDP/TLS/RTP/SAVPF 96\r\n",
              null,
              null,
              null,
              null,
              null);
      send(
          callerSession,
          "/app/calls/" + callId + "/signals",
          new RelaySignalRequest(callerInstance, offer));

      assertThat(received.get(10, SECONDS))
          .satisfies(
              envelope -> {
                assertThat(envelope.callId()).isEqualTo(callId);
                assertThat(envelope.senderId()).isEqualTo(caller);
                assertThat(envelope.signal()).isEqualTo(offer);
              });
    } finally {
      disconnect(callerSession);
      disconnect(calleeSession);
      client.stop();
    }
  }

  private Call connectingVideoCall(UUID caller, UUID callee) {
    var now = Instant.now();
    var call =
        Call.start(
            new CallParticipants(caller, callee),
            CallMediaType.VIDEO,
            UUID.randomUUID(),
            "fingerprint",
            now,
            now.plusSeconds(45));
    call.accept(callee, now.plusMillis(1), now.plusSeconds(30));
    return call;
  }

  private StompSession connect(WebSocketStompClient client, UUID userId) throws Exception {
    var headers = new StompHeaders();
    headers.set(
        HttpHeaders.AUTHORIZATION, "Bearer " + accessTokens.createAccessToken(new UserId(userId)));
    return client
        .connectAsync(
            webSocketUrl(),
            new WebSocketHttpHeaders(),
            headers,
            new StompSessionHandlerAdapter() {})
        .get(10, SECONDS);
  }

  private void register(StompSession session, UUID instanceId) throws Exception {
    var ready = new CompletableFuture<CallStompResponse>();
    var subscription =
        session.subscribe("/user/queue/calls/events", frameHandler(CallStompResponse.class, ready));
    send(session, "/app/calls/client-ready", new ClientReadyRequest(instanceId));
    assertThat(ready.get(10, SECONDS).type()).isEqualTo("CLIENT_READY");
    subscription.unsubscribe();
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

  private static void send(StompSession session, String destination, Object payload) {
    var headers = new StompHeaders();
    headers.setDestination(destination);
    headers.setContentType(org.springframework.http.MediaType.APPLICATION_JSON);
    session.send(headers, payload);
  }

  private static void disconnect(StompSession session) {
    if (session != null && session.isConnected()) session.disconnect();
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

  private record SignalEnvelope(UUID callId, UUID senderId, CallSignal signal) {}
}
