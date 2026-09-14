package dev.amir.synapse.identity.infrastructure.adapter.out.persistence.user;

import dev.amir.synapse.identity.domain.value_object.Email;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserJpaRepository extends JpaRepository<UserEntity, UUID> {
  Optional<UserEntity> findByGoogleId(String googleId);

  Optional<UserEntity> findByEmail(Email email);
}
