package dev.amir.synapse.identity.domain.port.out.refresh_token;

import dev.amir.synapse.identity.domain.model.RefreshToken;

@FunctionalInterface
public interface RevokeRefreshTokenPort {
  void save(RefreshToken token); // persists the revoked state
}
