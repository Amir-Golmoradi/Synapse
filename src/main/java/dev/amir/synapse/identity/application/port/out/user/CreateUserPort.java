package dev.amir.synapse.identity.application.port.out.user;

import dev.amir.synapse.identity.domain.model.User;

/** Insert-only persistence boundary for a newly registered user. */
@FunctionalInterface
public interface CreateUserPort {
  User create(User user);
}
