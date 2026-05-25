package dev.amir.synapse.identity.infrastructure.adapter.in.persistence.user;

import dev.amir.synapse.identity.domain.model.User;
import dev.amir.synapse.identity.domain.value_object.Email;
import dev.amir.synapse.identity.domain.value_object.FullName;
import dev.amir.synapse.identity.domain.value_object.UserId;
import org.springframework.stereotype.Service;

@Service
public class UserMapper {

  public User toDomain(UserEntity entity) {
    return User.reconstitute(
        new UserId(entity.id),
        Email.of(entity.email),
        entity.googleId,
        FullName.of(entity.firstName, entity.lastName),
        entity.profilePictureUrl);
  }

  public UserEntity toEntity(User user) {
    var entity = new UserEntity();
    var fullName = user.getFullName();

    entity.id = user.getId().getValue();
    entity.googleId = user.getGoogleId();
    entity.email = user.getEmail().getValue();
    entity.firstName = fullName.getFirstName();
    entity.lastName = fullName.getLastName();
    entity.profilePictureUrl = user.getProfilePictureUrl();
    return entity;
  }
}
