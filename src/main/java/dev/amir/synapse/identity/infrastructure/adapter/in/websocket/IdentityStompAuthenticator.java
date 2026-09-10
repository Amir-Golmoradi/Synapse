package dev.amir.synapse.identity.infrastructure.adapter.in.websocket;

import dev.amir.synapse.identity.domain.port.in.access_token.AuthenticateAccessTokenQuery;
import dev.amir.synapse.identity.domain.port.in.access_token.AuthenticateAccessTokenUseCase;
import dev.amir.synapse.shared.websocket.api.StompAuthenticator;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class IdentityStompAuthenticator implements StompAuthenticator {
  private final AuthenticateAccessTokenUseCase authenticateAccessToken;

  public IdentityStompAuthenticator(AuthenticateAccessTokenUseCase authenticateAccessToken) {
    this.authenticateAccessToken = authenticateAccessToken;
  }

  @Override
  public Optional<UUID> authenticate(String accessToken) {
    return authenticateAccessToken
        .handle(new AuthenticateAccessTokenQuery(accessToken))
        .map(userId -> userId.getValue());
  }
}
