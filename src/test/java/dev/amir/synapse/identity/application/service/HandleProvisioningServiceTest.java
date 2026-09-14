package dev.amir.synapse.identity.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import dev.amir.synapse.identity.application.exception.AccountConflictException;
import dev.amir.synapse.identity.application.exception.EmailConflictException;
import dev.amir.synapse.identity.application.exception.GoogleSubjectConflictException;
import dev.amir.synapse.identity.application.exception.HandleConflictException;
import dev.amir.synapse.identity.application.exception.HandleProvisioningExhaustedException;
import dev.amir.synapse.identity.application.port.out.oauth.VerifiedOidcProfile;
import dev.amir.synapse.identity.application.port.out.user.CreateUserPort;
import dev.amir.synapse.identity.application.port.out.user.LoadUserPort;
import dev.amir.synapse.identity.application.port.out.user_search.UserSearchCachePort;
import dev.amir.synapse.identity.domain.model.User;
import dev.amir.synapse.identity.domain.value_object.DisplayName;
import dev.amir.synapse.identity.domain.value_object.Email;
import dev.amir.synapse.identity.domain.value_object.Handle;
import dev.amir.synapse.identity.domain.value_object.UserId;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class HandleProvisioningServiceTest {
  private final CreateUserPort createUser = mock(CreateUserPort.class);
  private final LoadUserPort loadUser = mock(LoadUserPort.class);
  private final UserSearchCachePort cache = mock(UserSearchCachePort.class);
  private HandleProvisioningService service;

  @BeforeEach
  void setUp() {
    service = new HandleProvisioningService(createUser, loadUser, cache);
  }

  @Test
  void retriesInExactOrderWithOneStableUserId() {
    when(createUser.create(any()))
        .thenThrow(new HandleConflictException(new RuntimeException()))
        .thenThrow(new HandleConflictException(new RuntimeException()))
        .thenAnswer(invocation -> invocation.getArgument(0));

    var created = service.provision(profile("Amir", "google-1", "amir@example.com"));

    var attempts = ArgumentCaptor.forClass(User.class);
    verify(createUser, times(3)).create(attempts.capture());
    assertThat(attempts.getAllValues())
        .extracting(user -> user.getHandle().value())
        .containsExactly("amir", "amir_1", "amir_2");
    assertThat(attempts.getAllValues())
        .extracting(User::getId)
        .containsOnly(attempts.getAllValues().getFirst().getId());
    assertThat(created.getHandle().value()).isEqualTo("amir_2");
    verify(cache).incrementGeneration();
  }

  @Test
  void fallbackCollisionReturnsServiceUnavailableAndDoesNotInvalidateCache() {
    when(createUser.create(any())).thenThrow(new HandleConflictException(new RuntimeException()));

    assertThatThrownBy(() -> service.provision(profile("Amir", "google-2", "other@example.com")))
        .isInstanceOf(HandleProvisioningExhaustedException.class);

    var attempts = ArgumentCaptor.forClass(User.class);
    verify(createUser, times(7)).create(attempts.capture());
    assertThat(attempts.getAllValues())
        .extracting(user -> user.getHandle().value())
        .containsExactly(
            "amir",
            "amir_1",
            "amir_2",
            "amir_3",
            "amir_4",
            "amir_5",
            attempts.getAllValues().getLast().getHandle().value());
    assertThat(attempts.getAllValues().getLast().getHandle().value()).startsWith("u_");
    assertThat(attempts.getAllValues())
        .extracting(User::getId)
        .containsOnly(attempts.getAllValues().getFirst().getId());
    verify(cache, never()).incrementGeneration();
  }

  @Test
  void unicodeOnlyDisplayNameAttemptsUuidFallbackImmediately() {
    when(createUser.create(any())).thenAnswer(invocation -> invocation.getArgument(0));

    var created = service.provision(profile("امیر", "google-3", "persian@example.com"));

    assertThat(created.getHandle().value()).startsWith("u_").hasSize(27);
    verify(createUser).create(any());
  }

  @Test
  void cacheOutageCannotBreakSuccessfulUserCreation() {
    when(createUser.create(any())).thenAnswer(invocation -> invocation.getArgument(0));
    doThrow(new IllegalStateException("redis unavailable")).when(cache).incrementGeneration();

    assertThat(service.provision(profile("Amir", "google-cache", "cache@example.com")))
        .extracting(user -> user.getHandle().value())
        .isEqualTo("amir");
  }

  @Test
  void googleSubjectConflictReloadsWinningUserWithoutCacheInvalidation() {
    var winner = user("google-4", "winner@example.com", "winner");
    when(createUser.create(any()))
        .thenThrow(new GoogleSubjectConflictException(new RuntimeException()));
    when(loadUser.findByGoogleId("google-4")).thenReturn(Optional.of(winner));

    assertThat(service.provision(profile("Winner", "google-4", "winner@example.com")))
        .isSameAs(winner);
    verify(cache, never()).incrementGeneration();
  }

  @Test
  void emailConflictReloadsSameSubjectBeforeRejectingAccount() {
    var winner = user("google-5", "same@example.com", "same_user");
    when(createUser.create(any())).thenThrow(new EmailConflictException(new RuntimeException()));
    when(loadUser.findByGoogleId("google-5")).thenReturn(Optional.of(winner));

    assertThat(service.provision(profile("Same", "google-5", "same@example.com"))).isSameAs(winner);
  }

  @Test
  void emailOwnedByDifferentSubjectReturnsExplicitAccountConflict() {
    var email = Email.of("shared@example.com");
    when(createUser.create(any())).thenThrow(new EmailConflictException(new RuntimeException()));
    when(loadUser.findByGoogleId("google-new")).thenReturn(Optional.empty());
    when(loadUser.findByEmail(email))
        .thenReturn(Optional.of(user("google-old", email.value(), "old_user")));

    assertThatThrownBy(() -> service.provision(profile("New", "google-new", email.value())))
        .isInstanceOf(AccountConflictException.class)
        .extracting(exception -> ((AccountConflictException) exception).getHttpStatus())
        .isEqualTo(409);
    verify(loadUser).findByEmail(email);
  }

  private static VerifiedOidcProfile profile(String displayName, String subject, String email) {
    return new VerifiedOidcProfile(
        "google", subject, Email.of(email), DisplayName.of(displayName), null);
  }

  private static User user(String subject, String email, String handle) {
    return User.reconstitute(
        UserId.generate(),
        Email.of(email),
        subject,
        Handle.of(handle),
        DisplayName.of("Existing"),
        null);
  }
}
