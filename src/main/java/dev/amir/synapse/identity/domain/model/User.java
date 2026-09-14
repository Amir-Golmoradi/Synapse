package dev.amir.synapse.identity.domain.model;

import dev.amir.synapse.identity.domain.event.UserRegisteredEvent;
import dev.amir.synapse.identity.domain.value_object.DisplayName;
import dev.amir.synapse.identity.domain.value_object.Email;
import dev.amir.synapse.identity.domain.value_object.Handle;
import dev.amir.synapse.identity.domain.value_object.UserId;
import dev.amir.synapse.shared.domain.AggregateRoot;
import dev.amir.synapse.shared.domain.DomainEvent;
import java.util.Objects;
import org.jspecify.annotations.Nullable;

/** Identity aggregate for a Google-backed Synapse user. */
public class User extends AggregateRoot<UserId, DomainEvent> {
  private final Email email;
  private final String googleId;
  private final Handle handle;
  private DisplayName displayName;
  private @Nullable String profilePictureUrl;

  private User(
      UserId id,
      Email email,
      String googleId,
      Handle handle,
      DisplayName displayName,
      @Nullable String profilePictureUrl) {
    super(Objects.requireNonNull(id, "id"));
    this.email = Objects.requireNonNull(email, "email");
    this.googleId = Objects.requireNonNull(googleId, "googleId");
    this.handle = Objects.requireNonNull(handle, "handle");
    this.displayName = Objects.requireNonNull(displayName, "displayName");
    this.profilePictureUrl = profilePictureUrl;
  }

  public static User registerViaGoogle(
      UserId userId,
      Email email,
      String googleSubjectId,
      Handle handle,
      DisplayName displayName,
      @Nullable String profilePictureUrl) {
    var user = new User(userId, email, googleSubjectId, handle, displayName, profilePictureUrl);
    user.registerEvent(new UserRegisteredEvent(user.getId(), user.email));
    return user;
  }

  public static User reconstitute(
      UserId userId,
      Email email,
      String googleId,
      Handle handle,
      DisplayName displayName,
      @Nullable String profilePictureUrl) {
    return new User(userId, email, googleId, handle, displayName, profilePictureUrl);
  }

  public void syncGoogleProfile(
      DisplayName updatedDisplayName, @Nullable String updatedProfilePictureUrl) {
    displayName = Objects.requireNonNull(updatedDisplayName, "updatedDisplayName");
    profilePictureUrl = updatedProfilePictureUrl;
  }

  public Email getEmail() {
    return email;
  }

  public String getGoogleId() {
    return googleId;
  }

  public DisplayName getDisplayName() {
    return displayName;
  }

  public Handle getHandle() {
    return handle;
  }

  public @Nullable String getProfilePictureUrl() {
    return profilePictureUrl;
  }

  @Override
  public boolean equals(Object other) {
    if (this == other) {
      return true;
    }
    if (!(other instanceof User user)) {
      return false;
    }
    return Objects.equals(getId(), user.getId());
  }

  @Override
  public int hashCode() {
    return Objects.hash(getId());
  }
}
