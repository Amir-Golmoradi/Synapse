package dev.amir.synapse.identity.domain.port.out.refresh_token;

import dev.amir.synapse.identity.domain.model.RefreshToken;

@FunctionalInterface
public interface SaveRefreshTokenPort {
  void save(RefreshToken token);
}
