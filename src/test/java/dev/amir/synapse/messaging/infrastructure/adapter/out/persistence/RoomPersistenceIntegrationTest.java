package dev.amir.synapse.messaging.infrastructure.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import dev.amir.synapse.messaging.domain.enums.RoomStatus;
import dev.amir.synapse.messaging.domain.model.Room;
import dev.amir.synapse.messaging.domain.port.out.LoadRoomPort;
import dev.amir.synapse.messaging.domain.port.out.SaveRoomPort;
import dev.amir.synapse.messaging.domain.value_object.MemberId;
import jakarta.persistence.EntityManager;
import java.time.temporal.ChronoUnit;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Transactional;
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
class RoomPersistenceIntegrationTest {

  @Container
  private static final PostgreSQLContainer<?> POSTGRES =
      new PostgreSQLContainer<>("postgres:16-alpine")
          .withDatabaseName("synapse_test")
          .withUsername("synapse")
          .withPassword("synapse");

  @Autowired private SaveRoomPort saveRoomPort;

  @Autowired private LoadRoomPort loadRoomPort;

  @Autowired private EntityManager entityManager;

  @DynamicPropertySource
  static void registerDatasourceProperties(DynamicPropertyRegistry registry) {
    registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
    registry.add("spring.datasource.username", POSTGRES::getUsername);
    registry.add("spring.datasource.password", POSTGRES::getPassword);
  }

  @Test
  @Transactional
  void roundTripPreservesDomainManagedStatusAndTimestamps() throws InterruptedException {
    var room = Room.createGroupRoom(MemberId.generate(), "Persistence", null);
    var createdAt = room.getCreatedAt();

    Thread.sleep(5);
    room.recordMessageActivity();
    var lastMessagesAt = room.getLastMessagesAt();
    room.archive();

    saveRoomPort.save(room);
    entityManager.flush();
    entityManager.clear();

    var reloaded = loadRoomPort.findById(room.getId().getValue()).orElseThrow();

    assertThat(reloaded.getStatus()).isEqualTo(RoomStatus.ARCHIVED);
    assertThat(reloaded.getCreatedAt()).isCloseTo(createdAt, within(1, ChronoUnit.MILLIS));
    assertThat(reloaded.getLastMessagesAt())
        .isCloseTo(lastMessagesAt, within(1, ChronoUnit.MILLIS));
    assertThat(reloaded.getLastMessagesAt()).isAfter(reloaded.getCreatedAt());
  }
}
