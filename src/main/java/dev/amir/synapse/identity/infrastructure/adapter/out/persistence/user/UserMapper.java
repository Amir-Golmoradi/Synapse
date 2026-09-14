package dev.amir.synapse.identity.infrastructure.adapter.out.persistence.user;

import dev.amir.synapse.identity.domain.model.User;
import dev.amir.synapse.identity.domain.value_object.DisplayName;
import dev.amir.synapse.identity.domain.value_object.Handle;
import dev.amir.synapse.identity.domain.value_object.UserId;
import org.springframework.stereotype.Service;

@Service
public class UserMapper {

  public User toDomain(UserEntity entity) {
    return User.reconstitute(
        UserId.of(entity.getId().toString()),
        entity.getEmail(),
        entity.getGoogleId(),
        Handle.of(entity.getHandle()),
        DisplayName.of(entity.getDisplayName()),
        entity.getProfilePictureUrl());
  }

  public UserEntity toEntity(User user) {
    var entity = new UserEntity();
    var displayName = user.getDisplayName();

    entity.id = user.getId().getValue();
    entity.googleId = user.getGoogleId();
    entity.email = user.getEmail();
    entity.displayName = displayName.getValue();
    entity.handle = user.getHandle().value();
    entity.profilePictureUrl = user.getProfilePictureUrl();
    return entity;
  }
}
