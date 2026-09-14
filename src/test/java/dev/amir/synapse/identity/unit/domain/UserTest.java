package dev.amir.synapse.identity.unit.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import dev.amir.synapse.identity.domain.model.User;
import dev.amir.synapse.identity.domain.value_object.DisplayName;
import dev.amir.synapse.identity.domain.value_object.Email;
import dev.amir.synapse.identity.domain.value_object.Handle;
import dev.amir.synapse.identity.domain.value_object.UserId;
import org.junit.jupiter.api.Test;

class UserTest {
  @Test
  void registersGoogleUserFromExplicitDomainValues() {
    var userId = UserId.generate();
    var handle = Handle.of("amir_gm");

    var user = register(userId, handle, "Amir Gm", "avatar.png");

    assertThat(user.getId()).isEqualTo(userId);
    assertThat(user.getHandle()).isEqualTo(handle);
    assertThat(user.getGoogleId()).isEqualTo("google-123");
  }

  @Test
  void reconstitutesPersistedHandle() {
    var handle = Handle.of("persisted_user");
    var user =
        User.reconstitute(
            UserId.generate(),
            Email.of("amir@example.com"),
            "google-123",
            handle,
            DisplayName.of("Amir Gm"),
            null);

    assertThat(user.getHandle()).isEqualTo(handle);
  }

  @Test
  void keepsHandleStableWhenSynchronizingGoogleProfile() {
    var handle = Handle.of("amir_gm");
    var user = register(UserId.generate(), handle, "Amir Gm", "old.png");

    user.syncGoogleProfile(DisplayName.of("Amir Updated"), null);

    assertThat(user.getHandle()).isEqualTo(handle);
    assertThat(user.getDisplayName().value()).isEqualTo("Amir Updated");
    assertThat(user.getProfilePictureUrl()).isNull();
  }

  @Test
  void rejectsMissingHandle() {
    assertThatThrownBy(
            () ->
                User.registerViaGoogle(
                    UserId.generate(),
                    Email.of("amir@example.com"),
                    "google-123",
                    null,
                    DisplayName.of("Amir"),
                    null))
        .isInstanceOf(NullPointerException.class);
  }

  private static User register(
      UserId userId, Handle handle, String displayName, String profilePictureUrl) {
    return User.registerViaGoogle(
        userId,
        Email.of("amir@example.com"),
        "google-123",
        handle,
        DisplayName.of(displayName),
        profilePictureUrl);
  }
}
