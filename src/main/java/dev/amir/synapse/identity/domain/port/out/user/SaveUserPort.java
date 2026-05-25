package dev.amir.synapse.identity.domain.port.out.user;

import dev.amir.synapse.identity.domain.model.User;

@FunctionalInterface
public interface SaveUserPort {
  User save(User user);
}
