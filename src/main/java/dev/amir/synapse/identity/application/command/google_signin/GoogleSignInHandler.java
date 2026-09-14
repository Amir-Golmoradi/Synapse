package dev.amir.synapse.identity.application.command.google_signin;

import dev.amir.synapse.identity.application.exception.OidcVerificationException;
import dev.amir.synapse.identity.application.port.out.access_token.CreateAccessTokenPort;
import dev.amir.synapse.identity.application.port.out.oauth.OidcPort;
import dev.amir.synapse.identity.application.port.out.oauth.VerifiedOidcProfile;
import dev.amir.synapse.identity.application.port.out.refresh_token.SaveRefreshTokenPort;
import dev.amir.synapse.identity.application.port.out.user.LoadUserPort;
import dev.amir.synapse.identity.application.port.out.user.SaveUserPort;
import dev.amir.synapse.identity.application.service.HandleProvisioningService;
import dev.amir.synapse.identity.domain.entity.RefreshToken;
import dev.amir.synapse.identity.domain.model.User;
import dev.amir.synapse.identity.domain.port.in.google_signin.GoogleSignInCommand;
import dev.amir.synapse.identity.domain.port.in.google_signin.GoogleSignInResult;
import dev.amir.synapse.identity.domain.port.in.google_signin.GoogleSignInUseCase;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class GoogleSignInHandler implements GoogleSignInUseCase {
  private static final String SUPPORTED_PROVIDER = "google";

  private final CreateAccessTokenPort createAccessToken;
  private final LoadUserPort loadUser;
  private final SaveUserPort saveUser;
  private final OidcPort oidc;
  private final SaveRefreshTokenPort saveRefreshToken;
  private final Duration refreshTokenValidity;
  private final HandleProvisioningService handleProvisioning;

  public GoogleSignInHandler(
      CreateAccessTokenPort createAccessToken,
      LoadUserPort loadUser,
      SaveUserPort saveUser,
      OidcPort oidc,
      SaveRefreshTokenPort saveRefreshToken,
      @Value("${synapse.refresh-token.validity-days:30}") int validityDays,
      HandleProvisioningService handleProvisioning) {
    this.createAccessToken = createAccessToken;
    this.loadUser = loadUser;
    this.saveUser = saveUser;
    this.oidc = oidc;
    this.saveRefreshToken = saveRefreshToken;
    refreshTokenValidity = Duration.ofDays(validityDays);
    this.handleProvisioning = handleProvisioning;
  }

  @Override
  public GoogleSignInResult handle(GoogleSignInCommand command) {
    var profile = oidc.verifyIdToken(command.googleIdToken());
    requireGoogle(profile);

    var user =
        loadUser
            .findByGoogleId(profile.subjectId())
            .map(existing -> updateExistingUser(existing, profile))
            .orElseGet(() -> handleProvisioning.provision(profile));

    var accessToken = createAccessToken.createAccessToken(user.getId());
    var issuedRefreshToken = RefreshToken.issue(user.getId(), refreshTokenValidity);
    saveRefreshToken.save(issuedRefreshToken.token());

    return new GoogleSignInResult(
        user.getId().value().toString(),
        user.getHandle().value(),
        accessToken,
        issuedRefreshToken.rawToken(),
        user.getDisplayName().value(),
        user.getProfilePictureUrl());
  }

  private static void requireGoogle(VerifiedOidcProfile profile) {
    if (!SUPPORTED_PROVIDER.equals(profile.provider())) {
      throw OidcVerificationException.unsupportedProvider(profile.provider());
    }
  }

  private User updateExistingUser(User existing, VerifiedOidcProfile profile) {
    existing.syncGoogleProfile(profile.displayName(), profile.profilePictureUrl());
    return saveUser.save(existing);
  }
}
