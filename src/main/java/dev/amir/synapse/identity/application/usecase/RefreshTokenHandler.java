package dev.amir.synapse.identity.application.usecase;

import dev.amir.synapse.identity.domain.exception.InvalidRefreshTokenException;
import dev.amir.synapse.identity.domain.model.RefreshToken;
import dev.amir.synapse.identity.domain.port.in.refresh_token.RefreshTokenCommand;
import dev.amir.synapse.identity.domain.port.in.refresh_token.RefreshTokenResult;
import dev.amir.synapse.identity.domain.port.in.refresh_token.RefreshTokenUseCase;
import dev.amir.synapse.identity.domain.port.out.refresh_token.LoadRefreshTokenPort;
import dev.amir.synapse.identity.domain.port.out.refresh_token.RevokeRefreshTokenPort;
import dev.amir.synapse.identity.domain.port.out.refresh_token.SaveRefreshTokenPort;
import dev.amir.synapse.identity.domain.port.out.token.CreateAccessTokenPort;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class RefreshTokenHandler implements RefreshTokenUseCase {

  private final CreateAccessTokenPort createAccessTokenPort;
  private final LoadRefreshTokenPort loadRefreshToken;
  private final RevokeRefreshTokenPort revokeRefreshToken;
  private final SaveRefreshTokenPort saveRefreshToken;
  private final Duration refreshTokenValidity;

  public RefreshTokenHandler(
      CreateAccessTokenPort createAccessTokenPort,
      LoadRefreshTokenPort loadRefreshToken,
      RevokeRefreshTokenPort revokeRefreshToken,
      SaveRefreshTokenPort saveRefreshToken,
      @Value("${synapse.refresh-token.validity-days:30}") int validityDays) {
    this.createAccessTokenPort = createAccessTokenPort;
    this.loadRefreshToken = loadRefreshToken;
    this.revokeRefreshToken = revokeRefreshToken;
    this.saveRefreshToken = saveRefreshToken;
    this.refreshTokenValidity = Duration.ofDays(validityDays);
  }

  @Override
  public RefreshTokenResult handle(RefreshTokenCommand command) {
    var hash = RefreshToken.hash(command.refreshToken());

    var existing =
        loadRefreshToken
            .findByTokenHash(hash)
            .orElseThrow(() -> new InvalidRefreshTokenException("Refresh token not found"));

    // Validates expiry + revoked state — throws if invalid
    existing.validate();

    // Rotate: revoke old, issue new
    existing.revoke();
    revokeRefreshToken.save(existing);

    var issued = RefreshToken.issue(existing.getUserId(), refreshTokenValidity);
    saveRefreshToken.save(issued.token());

    String accessToken = createAccessTokenPort.createAccessToken(existing.getUserId());
    return new RefreshTokenResult(accessToken, command.refreshToken());
  }
}
