package dev.amir.synapse.identity.domain.port.out.refresh_token;

import dev.amir.synapse.identity.domain.model.RefreshToken;
import java.util.Optional;

@FunctionalInterface
public interface LoadRefreshTokenPort {
  Optional<RefreshToken> findByTokenHash(String tokenHash);
}
