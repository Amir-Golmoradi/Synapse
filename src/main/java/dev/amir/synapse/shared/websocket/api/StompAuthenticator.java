package dev.amir.synapse.shared.websocket.api;

import java.util.Optional;
import java.util.UUID;

/** Authenticates a STOMP CONNECT token without coupling shared transport code to Identity. */
public interface StompAuthenticator {
  Optional<UUID> authenticate(String accessToken);
}
