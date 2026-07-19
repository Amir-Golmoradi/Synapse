package dev.amir.synapse.identity.application.command.google_signin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import dev.amir.synapse.identity.application.exception.OidcVerificationException;
import dev.amir.synapse.identity.application.port.out.access_token.CreateAccessTokenPort;
import dev.amir.synapse.identity.application.port.out.oauth.OidcPort;
import dev.amir.synapse.identity.application.port.out.oauth.VerifiedOidcProfile;
import dev.amir.synapse.identity.application.port.out.refresh_token.SaveRefreshTokenPort;
import dev.amir.synapse.identity.application.port.out.user.LoadUserPort;
import dev.amir.synapse.identity.application.port.out.user.SaveUserPort;
import dev.amir.synapse.identity.application.service.HandleProvisioningService;
import dev.amir.synapse.identity.domain.model.User;
import dev.amir.synapse.identity.domain.port.in.google_signin.GoogleSignInCommand;
import dev.amir.synapse.identity.domain.value_object.DisplayName;
import dev.amir.synapse.identity.domain.value_object.Email;
import dev.amir.synapse.identity.domain.value_object.Handle;
import dev.amir.synapse.identity.domain.value_object.UserId;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class GoogleSignInHandlerTest {
  private final CreateAccessTokenPort accessToken = mock(CreateAccessTokenPort.class);
  private final LoadUserPort loadUser = mock(LoadUserPort.class);
  private final SaveUserPort saveUser = mock(SaveUserPort.class);
  private final OidcPort oidc = mock(OidcPort.class);
  private final SaveRefreshTokenPort saveRefreshToken = mock(SaveRefreshTokenPort.class);
  private final HandleProvisioningService provisioning = mock(HandleProvisioningService.class);
  private GoogleSignInHandler handler;

  @BeforeEach
  void setUp() {
    handler =
        new GoogleSignInHandler(
            accessToken, loadUser, saveUser, oidc, saveRefreshToken, 30, provisioning);
  }

  @Test
  void rejectsUnsupportedProviderAtApplicationBoundary() {
    when(oidc.verifyIdToken("token")).thenReturn(profile("github"));

    assertThatThrownBy(() -> handler.handle(new GoogleSignInCommand("token")))
        .isInstanceOf(OidcVerificationException.class);
    verify(loadUser, never()).findByGoogleId(any());
    verify(provisioning, never()).provision(any());
  }

  @Test
  void returningUserKeepsHandleAndSynchronizesNullableAvatar() {
    var profile = profile("google");
    var existing = user("stable_handle", "Old Name", "old.png");
    when(oidc.verifyIdToken("token")).thenReturn(profile);
    when(loadUser.findByGoogleId(profile.subjectId())).thenReturn(Optional.of(existing));
    when(saveUser.save(existing)).thenAnswer(invocation -> invocation.getArgument(0));
    when(accessToken.createAccessToken(existing.getId())).thenReturn("access-token");

    var result = handler.handle(new GoogleSignInCommand("token"));

    assertThat(result.handle()).isEqualTo("stable_handle");
    assertThat(result.displayName()).isEqualTo("Amir Updated");
    assertThat(result.profilePictureUrl()).isNull();
    assertThat(existing.getHandle().value()).isEqualTo("stable_handle");
    verify(saveRefreshToken).save(any());
  }

  @Test
  void newUserResultExposesAllocatedHandle() {
    var profile = profile("google");
    var created = user("amir", "Amir Updated", null);
    when(oidc.verifyIdToken("token")).thenReturn(profile);
    when(loadUser.findByGoogleId(profile.subjectId())).thenReturn(Optional.empty());
    when(provisioning.provision(profile)).thenReturn(created);
    when(accessToken.createAccessToken(created.getId())).thenReturn("access-token");

    assertThat(handler.handle(new GoogleSignInCommand("token")).handle()).isEqualTo("amir");
  }

  private static VerifiedOidcProfile profile(String provider) {
    return new VerifiedOidcProfile(
        provider,
        "google-subject",
        Email.of("amir@example.com"),
        DisplayName.of("Amir Updated"),
        null);
  }

  private static User user(String handle, String displayName, String picture) {
    return User.reconstitute(
        UserId.generate(),
        Email.of("amir@example.com"),
        "google-subject",
        Handle.of(handle),
        DisplayName.of(displayName),
        picture);
  }
}
