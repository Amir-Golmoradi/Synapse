package dev.amir.synapse.identity.domain.port.in.refresh_token;

@FunctionalInterface
public interface RefreshTokenUseCase {
  RefreshTokenResult handle(RefreshTokenCommand command);
}
