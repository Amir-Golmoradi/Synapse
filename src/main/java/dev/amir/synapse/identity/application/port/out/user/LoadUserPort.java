package dev.amir.synapse.identity.application.port.out.user;

import dev.amir.synapse.identity.domain.model.User;
import dev.amir.synapse.identity.domain.value_object.UserId;
import java.util.Optional;

public interface LoadUserPort {
  Optional<User> findById(UserId userId);

  Optional<User> findByGoogleId(String googleId);
}
