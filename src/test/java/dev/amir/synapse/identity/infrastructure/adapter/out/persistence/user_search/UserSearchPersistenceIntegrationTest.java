package dev.amir.synapse.identity.infrastructure.adapter.out.persistence.user_search;

import static org.assertj.core.api.Assertions.assertThat;

import dev.amir.synapse.identity.application.port.out.user.CreateUserPort;
import dev.amir.synapse.identity.application.port.out.user_search.UserSearchPort;
import dev.amir.synapse.identity.domain.model.User;
import dev.amir.synapse.identity.domain.port.in.user_search.UserSearchItem;
import dev.amir.synapse.identity.domain.port.in.user_search.UserSearchQuery;
import dev.amir.synapse.identity.domain.value_object.DisplayName;
import dev.amir.synapse.identity.domain.value_object.Email;
import dev.amir.synapse.identity.domain.value_object.Handle;
import dev.amir.synapse.identity.domain.value_object.UserId;
import dev.amir.synapse.identity.infrastructure.adapter.out.persistence.user.UserJpaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest(
    properties = {
      "server.port=0",
      "spring.jpa.hibernate.ddl-auto=validate",
      "spring.flyway.enabled=true",
      "spring.flyway.locations=classpath:db/create-table,classpath:db/alter-table",
      "spring.security.oauth2.client.registration.google.client-id=test-google-client-id",
      "spring.security.oauth2.client.registration.google.client-secret=test-google-client-secret",
      "synapse.google-token-url=http://localhost/tokeninfo?id_token={idToken}",
      "synapse.jwt.secret=01234567890123456789012345678901",
      "synapse.jwt.token-expiration-ms=900000"
    })
@Testcontainers
class UserSearchPersistenceIntegrationTest {

  @Container
  private static final PostgreSQLContainer<?> POSTGRES =
      new PostgreSQLContainer<>("postgres:16-alpine")
          .withDatabaseName("synapse_user_search_test")
          .withUsername("synapse")
          .withPassword("synapse");

  @Autowired private CreateUserPort createUser;

  @Autowired private UserSearchPort userSearch;

  @Autowired private UserJpaRepository users;

  @DynamicPropertySource
  static void registerDatasourceProperties(DynamicPropertyRegistry registry) {
    registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
    registry.add("spring.datasource.username", POSTGRES::getUsername);
    registry.add("spring.datasource.password", POSTGRES::getPassword);
  }

  @BeforeEach
  void clearUsers() {
    users.deleteAll();
  }

  @Test
  void treatsUnderscoresAndPeriodsAsLiteralPrefixCharacters() {
    create("ami_r", "Amir underscore");
    create("ami_ra", "Amira underscore");
    create("ami.r", "Amir period");
    create("amixr", "Amir letter");

    var underscoreResult = userSearch.search(new UserSearchQuery("AMI_", 0, 20));
    var periodResult = userSearch.search(new UserSearchQuery("AMI.", 0, 20));

    assertThat(underscoreResult.items())
        .extracting(UserSearchItem::handle)
        .containsExactly("ami_r", "ami_ra");
    assertThat(periodResult.items()).extracting(UserSearchItem::handle).containsExactly("ami.r");
  }

  @Test
  void ordersByHandleAndUsesLimitPlusOnePagination() {
    create("pagea", "Page A");
    create("pageb", "Page B");
    create("pagec", "Page C");

    var firstPage = userSearch.search(new UserSearchQuery("page", 0, 2));
    var secondPage = userSearch.search(new UserSearchQuery("page", 1, 2));

    assertThat(firstPage.items())
        .extracting(UserSearchItem::handle)
        .containsExactly("pagea", "pageb");
    assertThat(firstPage.hasNext()).isTrue();
    assertThat(secondPage.items()).extracting(UserSearchItem::handle).containsExactly("pagec");
    assertThat(secondPage.hasNext()).isFalse();
  }

  private void create(String handle, String displayName) {
    createUser.create(
        User.reconstitute(
            UserId.generate(),
            Email.of(handle.replace('.', 'p') + "@example.com"),
            "google-" + handle,
            Handle.of(handle),
            DisplayName.of(displayName),
            null));
  }
}
