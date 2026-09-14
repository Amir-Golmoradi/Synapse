package dev.amir.synapse.identity.infrastructure.adapter.out.persistence.user;

import dev.amir.synapse.identity.application.port.out.user.CreateUserPort;
import dev.amir.synapse.identity.application.port.out.user.LoadUserPort;
import dev.amir.synapse.identity.application.port.out.user.SaveUserPort;
import dev.amir.synapse.identity.domain.exception.UserNotFoundException;
import dev.amir.synapse.identity.domain.model.User;
import dev.amir.synapse.identity.domain.value_object.Email;
import dev.amir.synapse.identity.domain.value_object.UserId;
import jakarta.persistence.EntityManager;
import java.util.Optional;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class UserJpaAdapter implements LoadUserPort, SaveUserPort, CreateUserPort {
  private final UserMapper mapper;
  private final UserJpaRepository repository;
  private final EntityManager entityManager;
  private final UserConstraintViolationTranslator constraintTranslator =
      new UserConstraintViolationTranslator();

  public UserJpaAdapter(
      UserJpaRepository repository, UserMapper mapper, EntityManager entityManager) {
    this.repository = repository;
    this.mapper = mapper;
    this.entityManager = entityManager;
  }

  @Override
  @Transactional(readOnly = true)
  public Optional<User> findByGoogleId(String googleId) {
    return repository.findByGoogleId(googleId).map(mapper::toDomain);
  }

  @Override
  @Transactional(readOnly = true)
  public Optional<User> findByEmail(Email email) {
    return repository.findByEmail(email).map(mapper::toDomain);
  }

  @Override
  @Transactional(readOnly = true)
  public Optional<User> findById(UserId userId) {
    return repository.findById(userId.value()).map(mapper::toDomain);
  }

  @Override
  @Transactional
  public User save(User user) {
    var entity = entityManager.find(UserEntity.class, user.getId().value());
    if (entity == null) {
      throw new UserNotFoundException(user.getId());
    }
    entity.updateProfile(user.getDisplayName().value(), user.getProfilePictureUrl());
    entityManager.flush();
    return mapper.toDomain(entity);
  }

  @Override
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public User create(User user) {
    var entity = mapper.toEntity(user);
    try {
      entityManager.persist(entity);
      entityManager.flush();
      return mapper.toDomain(entity);
    } catch (RuntimeException exception) {
      throw constraintTranslator.translate(exception);
    }
  }
}
