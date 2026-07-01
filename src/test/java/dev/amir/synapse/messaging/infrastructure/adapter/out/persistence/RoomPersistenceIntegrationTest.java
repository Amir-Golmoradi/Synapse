package dev.amir.synapse.messaging.infrastructure.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import dev.amir.synapse.messaging.domain.enums.RoomStatus;
import dev.amir.synapse.messaging.domain.enums.RoomType;
import dev.amir.synapse.messaging.domain.model.Room;
import dev.amir.synapse.messaging.domain.port.out.ListRoomSummariesPort;
import dev.amir.synapse.messaging.domain.port.out.LoadRoomPort;
import dev.amir.synapse.messaging.domain.port.out.LoadRoomSummaryPort;
import dev.amir.synapse.messaging.domain.port.out.RoomSummaryProjection;
import dev.amir.synapse.messaging.domain.port.out.RoomSummarySearchCriteria;
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

  @Autowired private ListRoomSummariesPort listRoomSummariesPort;

  @Autowired private LoadRoomSummaryPort loadRoomSummaryPort;

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

  @Test
  @Transactional
  void listRoomSummariesReturnsMemberScopedActiveRoomsSortedByLastMessageActivity()
      throws InterruptedException {
    var user = MemberId.generate();
    var first = Room.createGroupRoom(user, "First", null);
    Thread.sleep(10);
    var second = Room.createChannel(user, "Second", null);
    var outsiderOnly = Room.createGroupRoom(MemberId.generate(), "Outsider", null);
    var archived = Room.createGroupRoom(user, "Archived", null);
    archived.archive();

    saveRoomPort.save(first);
    saveRoomPort.save(second);
    saveRoomPort.save(outsiderOnly);
    saveRoomPort.save(archived);
    entityManager.flush();
    entityManager.clear();

    var activePage =
        listRoomSummariesPort.findRoomSummaries(
            new RoomSummarySearchCriteria(user.getValue(), null, RoomStatus.ACTIVE, 0, 10));

    assertThat(activePage.totalElements()).isEqualTo(2);
    assertThat(activePage.totalPages()).isEqualTo(1);
    assertThat(activePage.items())
        .extracting(RoomSummaryProjection::roomId)
        .containsExactly(second.getId().getValue(), first.getId().getValue());
    assertThat(activePage.items())
        .allSatisfy(
            summary -> {
              assertThat(summary.status()).isEqualTo(RoomStatus.ACTIVE);
              assertThat(summary.memberCount()).isEqualTo(1);
            });

    var groupPage =
        listRoomSummariesPort.findRoomSummaries(
            new RoomSummarySearchCriteria(
                user.getValue(), RoomType.GROUP, RoomStatus.ACTIVE, 0, 10));

    assertThat(groupPage.items())
        .singleElement()
        .satisfies(
            summary -> {
              assertThat(summary.roomId()).isEqualTo(first.getId().getValue());
              assertThat(summary.type()).isEqualTo(RoomType.GROUP);
              assertThat(summary.name()).isEqualTo("First");
            });

    var archivedPage =
        listRoomSummariesPort.findRoomSummaries(
            new RoomSummarySearchCriteria(user.getValue(), null, RoomStatus.ARCHIVED, 0, 10));

    assertThat(archivedPage.items())
        .singleElement()
        .satisfies(
            summary -> {
              assertThat(summary.roomId()).isEqualTo(archived.getId().getValue());
              assertThat(summary.status()).isEqualTo(RoomStatus.ARCHIVED);
            });
  }

  @Test
  @Transactional
  void listRoomSummariesPaginatesResults() {
    var user = MemberId.generate();
    for (var i = 0; i < 12; i++) {
      saveRoomPort.save(Room.createGroupRoom(user, "Room " + i, null));
    }
    entityManager.flush();
    entityManager.clear();

    var firstPage =
        listRoomSummariesPort.findRoomSummaries(
            new RoomSummarySearchCriteria(user.getValue(), null, RoomStatus.ACTIVE, 0, 10));
    var secondPage =
        listRoomSummariesPort.findRoomSummaries(
            new RoomSummarySearchCriteria(user.getValue(), null, RoomStatus.ACTIVE, 1, 10));

    assertThat(firstPage.items()).hasSize(10);
    assertThat(firstPage.totalElements()).isEqualTo(12);
    assertThat(firstPage.totalPages()).isEqualTo(2);
    assertThat(secondPage.items()).hasSize(2);
    assertThat(secondPage.totalElements()).isEqualTo(12);
    assertThat(secondPage.totalPages()).isEqualTo(2);
  }

  @Test
  @Transactional
  void loadRoomSummaryByIdReturnsArchivedRoomOnlyForMembers() {
    var user = MemberId.generate();
    var archivedRoom = Room.createGroupRoom(user, "Archived", null);
    archivedRoom.archive();
    var outsiderOnlyRoom = Room.createGroupRoom(MemberId.generate(), "Outsider", null);

    saveRoomPort.save(archivedRoom);
    saveRoomPort.save(outsiderOnlyRoom);
    entityManager.flush();
    entityManager.clear();

    var found =
        loadRoomSummaryPort.findRoomSummaryByIdForMember(
            archivedRoom.getId().getValue(), user.getValue());

    assertThat(found)
        .hasValueSatisfying(
            summary -> {
              assertThat(summary.roomId()).isEqualTo(archivedRoom.getId().getValue());
              assertThat(summary.status()).isEqualTo(RoomStatus.ARCHIVED);
              assertThat(summary.name()).isEqualTo("Archived");
              assertThat(summary.memberCount()).isEqualTo(1);
            });
    assertThat(
            loadRoomSummaryPort.findRoomSummaryByIdForMember(
                outsiderOnlyRoom.getId().getValue(), user.getValue()))
        .isEmpty();
    assertThat(
            loadRoomSummaryPort.findRoomSummaryByIdForMember(
                MemberId.generate().getValue(), user.getValue()))
        .isEmpty();
  }
}
