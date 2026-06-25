package dev.amir.synapse.identity.application.command.logout;

import dev.amir.synapse.identity.application.port.out.refresh_token.LoadRefreshTokenPort;
import dev.amir.synapse.identity.application.port.out.refresh_token.RevokeRefreshTokenPort;
import dev.amir.synapse.identity.domain.entity.RefreshToken;
import dev.amir.synapse.identity.domain.exception.InvalidRefreshTokenException;
import dev.amir.synapse.identity.domain.port.in.logout.LogoutCommand;
import dev.amir.synapse.identity.domain.port.in.logout.LogoutUseCase;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LogoutHandler implements LogoutUseCase {

  private final LoadRefreshTokenPort loadRefreshToken;
  private final RevokeRefreshTokenPort revokeRefreshToken;

  public LogoutHandler(
      LoadRefreshTokenPort loadRefreshToken, RevokeRefreshTokenPort revokeRefreshToken) {
    this.loadRefreshToken = loadRefreshToken;
    this.revokeRefreshToken = revokeRefreshToken;
  }

  @Transactional
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
