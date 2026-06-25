package dev.amir.synapse.identity.application.port.out.refresh_token;

import dev.amir.synapse.identity.domain.entity.RefreshToken;
import java.util.Optional;

@FunctionalInterface
public interface LoadRefreshTokenPort {
  Optional<RefreshToken> findByTokenHash(String tokenHash);
}
