package dev.amir.synapse.identity.domain.model;

import dev.amir.synapse.identity.domain.event.UserRegisteredEvent;
import dev.amir.synapse.identity.domain.value_object.Email;
import dev.amir.synapse.identity.domain.value_object.FullName;
import dev.amir.synapse.identity.domain.value_object.UserId;
import dev.amir.synapse.shared.domain.AggregateRoot;
import dev.amir.synapse.shared.domain.DomainEvent;
import org.jspecify.annotations.Nullable;

public class User extends AggregateRoot<UserId, DomainEvent> {
  private final Email email;
  private final String googleId;
  private FullName fullName;
  private @Nullable String profilePictureUrl;

  private User(
      UserId id,
      Email email,
      String googleId,
      FullName fullName,
      @Nullable String profilePictureUrl) {
    super(id);
    this.email = email;
    this.googleId = googleId;
    this.fullName = fullName;
    this.profilePictureUrl = profilePictureUrl;
  }

  private User(
      Email email, String googleId, FullName fullName, @Nullable String profilePictureUrl) {
    this(UserId.generate(), email, googleId, fullName, profilePictureUrl);
  }

  public static User registerViaGoogle(GoogleUserInfo info) {
    var user = new User(info.email(), info.googleId(), info.fullName(), info.profilePictureUrl());
    user.registerEvent(new UserRegisteredEvent(user.getId(), user.email));
    return user;
  }

  // ── Factory: reconstruct from persistence (no events) ───────────────────
  public static User reconstitute(
      UserId id,
      Email email,
      String googleId,
      FullName fullName,
      @Nullable String profilePictureUrl) {
    return new User(id, email, googleId, fullName, profilePictureUrl);
  }

  // ── Behavior ───────────────────────────────────────────────────────────
  // Called on every returning sign-in — keeps profile in sync with Google

  public void syncGoogleProfile(GoogleUserInfo info) {
    this.fullName = info.fullName();
    this.profilePictureUrl = info.profilePictureUrl();
  }

  public Email getEmail() {
    return email;
  }

  public String getGoogleId() {
    return googleId;
  }

  public FullName getFullName() {
    return fullName;
  }

  public @Nullable String getProfilePictureUrl() {
    return profilePictureUrl;
  }
}
