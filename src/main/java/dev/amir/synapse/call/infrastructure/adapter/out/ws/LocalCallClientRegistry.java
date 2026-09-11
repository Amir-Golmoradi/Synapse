package dev.amir.synapse.call.infrastructure.adapter.out.ws;

import dev.amir.synapse.call.application.port.out.CallClientConnectionPort;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

@Component
public class LocalCallClientRegistry implements CallClientConnectionPort {
  private final ConcurrentHashMap<ClientKey, String> sessionsByClient = new ConcurrentHashMap<>();
  private final ConcurrentHashMap<String, ClientKey> clientsBySession = new ConcurrentHashMap<>();

  @Override
  public void register(UUID userId, UUID clientInstanceId, String sessionId) {
    var key = new ClientKey(userId, clientInstanceId);
    var previousSession = sessionsByClient.put(key, sessionId);
    clientsBySession.put(sessionId, key);
    if (previousSession != null && !previousSession.equals(sessionId)) {
      clientsBySession.remove(previousSession, key);
    }
  }

  @Override
  public void disconnect(String sessionId) {
    var key = clientsBySession.remove(sessionId);
    if (key != null) {
      sessionsByClient.remove(key, sessionId);
    }
  }

  @Override
  public boolean isConnected(UUID userId, UUID clientInstanceId) {
    return sessionsByClient.containsKey(new ClientKey(userId, clientInstanceId));
  }

  @Override
  public Optional<String> sessionId(UUID userId, UUID clientInstanceId) {
    return Optional.ofNullable(sessionsByClient.get(new ClientKey(userId, clientInstanceId)));
  }

  private record ClientKey(UUID userId, UUID clientInstanceId) {}
}
