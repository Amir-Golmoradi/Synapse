package dev.amir.synapse.identity.infrastructure.adapter.out.persistence.user;

import dev.amir.synapse.identity.application.port.out.user.LoadUserPort;
import dev.amir.synapse.identity.application.port.out.user.SaveUserPort;
import dev.amir.synapse.identity.domain.model.User;
import dev.amir.synapse.identity.domain.value_object.UserId;
import java.util.Optional;
import org.springframework.stereotype.Service;

@Service
public class UserJpaAdapter implements LoadUserPort, SaveUserPort {

  private final UserMapper mapper;
  private final UserJpaRepository repository;

  public UserJpaAdapter(UserJpaRepository repository, UserMapper mapper) {
    this.repository = repository;
    this.mapper = mapper;
  }

  @Override
  public Optional<User> findByGoogleId(String googleId) {
    return repository.findByGoogleId(googleId).map(mapper::toDomain);
  }

  @Override
  public Optional<User> findById(UserId userId) {
    return repository.findById(userId.getValue()).map(mapper::toDomain);
  }

  @Override
  public User save(User user) {
    var saved = repository.save(mapper.toEntity(user));
    return mapper.toDomain(saved);
  }
}
