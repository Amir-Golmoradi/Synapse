package dev.amir.synapse.call.infrastructure.adapter.out.ws;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;
import org.junit.jupiter.api.Test;

class LocalCallClientRegistryTest {
  @Test
  void replacementSessionIsNotRemovedByLateDisconnect() {
    var registry = new LocalCallClientRegistry();
    var user = UUID.randomUUID();
    var instance = UUID.randomUUID();

    registry.register(user, instance, "old-session");
    registry.register(user, instance, "new-session");
    registry.disconnect("old-session");

    assertThat(registry.sessionId(user, instance)).contains("new-session");
  }
}
