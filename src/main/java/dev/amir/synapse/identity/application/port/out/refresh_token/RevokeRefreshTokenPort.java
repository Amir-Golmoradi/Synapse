package dev.amir.synapse.identity.application.port.out.refresh_token;

import dev.amir.synapse.identity.domain.entity.RefreshToken;

@FunctionalInterface
public interface RevokeRefreshTokenPort {
  void save(RefreshToken token); // persists the revoked state
}
