package dev.amir.synapse.identity.application.command.google_signin;

import dev.amir.synapse.identity.application.port.out.access_token.CreateAccessTokenPort;
import dev.amir.synapse.identity.application.port.out.google.GoogleOAuthPort;
import dev.amir.synapse.identity.application.port.out.refresh_token.SaveRefreshTokenPort;
import dev.amir.synapse.identity.application.port.out.user.LoadUserPort;
import dev.amir.synapse.identity.application.port.out.user.SaveUserPort;
import dev.amir.synapse.identity.domain.entity.RefreshToken;
import dev.amir.synapse.identity.domain.model.User;
import dev.amir.synapse.identity.domain.port.in.google_signin.GoogleSignInCommand;
import dev.amir.synapse.identity.domain.port.in.google_signin.GoogleSignInResult;
import dev.amir.synapse.identity.domain.port.in.google_signin.GoogleSignInUseCase;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GoogleSignInHandler implements GoogleSignInUseCase {
  private final CreateAccessTokenPort createAccessTokenPort;
  private final LoadUserPort loadUser;
  private final SaveUserPort saveUser;
  private final GoogleOAuthPort googleOAuth;
  private final SaveRefreshTokenPort saveRefreshToken;
  private final Duration refreshTokenValidity;

  public GoogleSignInHandler(
      CreateAccessTokenPort createAccessTokenPort,
      LoadUserPort loadUser,
      SaveUserPort saveUser,
      GoogleOAuthPort googleOAuth,
      SaveRefreshTokenPort saveRefreshToken,
      @Value("${synapse.refresh-token.validity-days:30}") int validityDays) {
    this.createAccessTokenPort = createAccessTokenPort;
    this.loadUser = loadUser;
    this.saveUser = saveUser;
    this.googleOAuth = googleOAuth;
    this.saveRefreshToken = saveRefreshToken;
    this.refreshTokenValidity = Duration.ofDays(validityDays);
  }

  @Transactional
  @Override
  public GoogleSignInResult handle(GoogleSignInCommand googleSignInCommand) {
    // 1.Verify with Google - Throws InvalidGoogleTokenException if bad.
    var info = googleOAuth.verifyIdToken(googleSignInCommand.googleIdToken());

    // 2. Find or Create user
    var user =
        loadUser
            .findByGoogleId(info.googleId())
            .map(
                existing -> {
                  existing.syncGoogleProfile(info);
                  return existing;
                })
            .orElseGet(() -> User.registerViaGoogle(info));
    var saved = saveUser.save(user);

    // Issue access token
    var accessToken = createAccessTokenPort.createAccessToken(saved.getId());

    var issued = RefreshToken.issue(saved.getId(), refreshTokenValidity);
    saveRefreshToken.save(issued.token());

    return new GoogleSignInResult(
        saved.getId().getValue().toString(),
        accessToken,
        issued.rawToken(),
        saved.getFullName().getFirstName(),
        saved.getFullName().getLastName(),
        saved.getProfilePictureUrl());
  }
}
