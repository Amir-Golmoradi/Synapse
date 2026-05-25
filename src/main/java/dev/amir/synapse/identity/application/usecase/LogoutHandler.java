package dev.amir.synapse.identity.application.usecase;

import dev.amir.synapse.identity.domain.exception.InvalidRefreshTokenException;
import dev.amir.synapse.identity.domain.model.RefreshToken;
import dev.amir.synapse.identity.domain.port.in.logout.LogoutCommand;
import dev.amir.synapse.identity.domain.port.in.logout.LogoutUseCase;
import dev.amir.synapse.identity.domain.port.out.refresh_token.LoadRefreshTokenPort;
import dev.amir.synapse.identity.domain.port.out.refresh_token.RevokeRefreshTokenPort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class LogoutHandler implements LogoutUseCase {

  private final LoadRefreshTokenPort loadRefreshToken;
  private final RevokeRefreshTokenPort revokeRefreshToken;

  public LogoutHandler(
      LoadRefreshTokenPort loadRefreshToken, RevokeRefreshTokenPort revokeRefreshToken) {
    this.loadRefreshToken = loadRefreshToken;
    this.revokeRefreshToken = revokeRefreshToken;
  }

  @Override
  public void handle(LogoutCommand command) {
    var hash = RefreshToken.hash(command.refreshToken());

    var token =
        loadRefreshToken
            .findByTokenHash(hash)
            .orElseThrow(() -> new InvalidRefreshTokenException("Refresh token not found"));

    token.revoke();
    revokeRefreshToken.save(token);
  }
}
