package dev.amir.synapse.call.infrastructure.adapter.in.ws;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.simp.stomp.StompCommand;

class CallStompDestinationPolicyTest {
  private final CallStompDestinationPolicy policy = new CallStompDestinationPolicy();

  @Test
  void supportsOnlyCallApplicationAndPrivateUserDestinations() {
    var callId = UUID.randomUUID();

    assertThat(policy.supports(StompCommand.SEND, "/app/calls/client-ready")).isTrue();
    assertThat(policy.supports(StompCommand.SEND, "/app/calls/" + callId + "/signals")).isTrue();
    assertThat(policy.supports(StompCommand.SUBSCRIBE, "/user/queue/calls/events")).isTrue();
    assertThat(policy.supports(StompCommand.SUBSCRIBE, "/topic/calls/" + callId)).isFalse();
    assertThat(policy.supports(StompCommand.SEND, "/app/calls/not-a-call/signals")).isFalse();
  }
}
