package dev.amir.synapse.identity.infrastructure.adapter.out.persistence.user;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "users")
public class UserEntity {

  @Id UUID id;

  @Column(nullable = false, unique = true)
  String googleId;

  @Column(nullable = false, unique = true)
  String email;

  @Column(nullable = false)
  String firstName;

  @Column(nullable = false)
  String lastName;

  @Column String profilePictureUrl;

  @Column(nullable = false, updatable = false)
  Instant createdAt;

  @Column(nullable = false)
  Instant updatedAt;

  public UUID getId() {
    return id;
  }

  public String getEmail() {
    return email;
  }

  public String getGoogleId() {
    return googleId;
  }

  public String getFirstName() {
    return firstName;
  }

  public String getLastName() {
    return lastName;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public Instant getUpdatedAt() {
    return updatedAt;
  }

  public String getProfilePictureUrl() {
    return profilePictureUrl;
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
