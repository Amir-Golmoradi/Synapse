package dev.amir.synapse.identity.infrastructure.adapter.out.persistence.user;

import dev.amir.synapse.identity.domain.value_object.Email;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

@Entity
@Table(
    name = "users",
    uniqueConstraints = {
      @UniqueConstraint(name = "uq_user_handle", columnNames = "handle"),
      @UniqueConstraint(name = "uq_user_email", columnNames = "email"),
      @UniqueConstraint(name = "uq_user_google_id", columnNames = "google_id"),
    })
public class UserEntity {

  @Id UUID id;

  @Column(name = "google_id", nullable = false, length = 64)
  String googleId;

  @Convert(converter = EmailAttributeConverter.class)
  @Column(nullable = false, length = 255)
  Email email;

  @Column(name = "display_name", nullable = false, length = 32)
  String displayName;

  @Column(nullable = false, length = 32)
  String handle;

  @Column(name = "profile_picture_url")
  @Nullable String profilePictureUrl;

  @Column(name = "created_at", nullable = false, updatable = false)
  Instant createdAt;

  @Column(name = "updated_at", nullable = false)
  Instant updatedAt;

  public UUID getId() {
    return id;
  }

  public Email getEmail() {
    return email;
  }

  public String getGoogleId() {
    return googleId;
  }

  public String getDisplayName() {
    return displayName;
  }

  public String getHandle() {
    return handle;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public Instant getUpdatedAt() {
    return updatedAt;
  }

  public @Nullable String getProfilePictureUrl() {
    return profilePictureUrl;
  }

  void updateProfile(String updatedDisplayName, @Nullable String updatedProfilePictureUrl) {
    displayName = updatedDisplayName;
    profilePictureUrl = updatedProfilePictureUrl;
  }

  @PrePersist
  void onCreate() {
    createdAt = Instant.now();
    updatedAt = Instant.now();
  }

  @PreUpdate
  void onUpdate() {
    updatedAt = Instant.now();
  }
}
