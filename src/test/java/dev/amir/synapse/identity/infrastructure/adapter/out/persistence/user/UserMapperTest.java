package dev.amir.synapse.identity.infrastructure.adapter.out.persistence.user;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import dev.amir.synapse.identity.domain.exception.InvalidHandleException;
import dev.amir.synapse.identity.domain.model.User;
import dev.amir.synapse.identity.domain.value_object.DisplayName;
import dev.amir.synapse.identity.domain.value_object.Email;
import dev.amir.synapse.identity.domain.value_object.Handle;
import dev.amir.synapse.identity.domain.value_object.UserId;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class UserMapperTest {
  private final UserMapper mapper = new UserMapper();

  @Test
  void shouldMapHandleFromDomainToEntity() {
    var user =
        User.reconstitute(
            UserId.generate(),
            Email.of("amir@example.com"),
            "google-123",
            Handle.of("amir_gm"),
            DisplayName.of("Amir Gm"),
            "https://example.com/avatar.png");

    var entity = mapper.toEntity(user);

    assertEquals("amir_gm", entity.getHandle());
  }

  @Test
  void shouldMapHandleFromEntityToDomain() {
    var entity = userEntity("amir_gm");

    var user = mapper.toDomain(entity);

    assertEquals(Handle.of("amir_gm"), user.getHandle());
  }

  @Test
  void shouldPreserveEmailValueObjectAcrossMapping() {
    var entity = userEntity("amir_gm");

    var user = mapper.toDomain(entity);
    var mappedEntity = mapper.toEntity(user);

    assertEquals(entity.getEmail(), user.getEmail());
    assertEquals(user.getEmail(), mappedEntity.getEmail());
  }

  @Test
  void shouldRejectEntityWithoutHandle() {
    var entity = userEntity(null);

    assertThrows(InvalidHandleException.class, () -> mapper.toDomain(entity));
  }

  @Test
  void shouldRejectEntityWithInvalidHandle() {
    var entity = userEntity("1-invalid");

    assertThrows(InvalidHandleException.class, () -> mapper.toDomain(entity));
  }

  private static UserEntity userEntity(String handle) {
    var entity = new UserEntity();
    entity.id = UUID.randomUUID();
    entity.googleId = "google-123";
    entity.email = Email.of("amir@example.com");
    entity.displayName = "Amir Gm";
    entity.handle = handle;
    entity.profilePictureUrl = "https://example.com/avatar.png";
    return entity;
  }
}
