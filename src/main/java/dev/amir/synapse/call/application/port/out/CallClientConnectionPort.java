package dev.amir.synapse.call.application.port.out;

import java.util.Optional;
import java.util.UUID;

public interface CallClientConnectionPort {
  void register(UUID userId, UUID clientInstanceId, String sessionId);

  void disconnect(String sessionId);

  boolean isConnected(UUID userId, UUID clientInstanceId);

  Optional<String> sessionId(UUID userId, UUID clientInstanceId);
}
