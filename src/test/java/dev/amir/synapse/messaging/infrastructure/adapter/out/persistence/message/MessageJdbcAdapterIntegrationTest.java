package dev.amir.synapse.messaging.infrastructure.adapter.out.persistence.message;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import dev.amir.synapse.messaging.domain.enums.RoomRole;
import dev.amir.synapse.messaging.domain.exception.MessageIdempotencyConflictException;
import dev.amir.synapse.messaging.domain.exception.MessageRoomAccessDeniedException;
import dev.amir.synapse.messaging.domain.model.Room;
import dev.amir.synapse.messaging.domain.port.in.list_messages.ListMessagesQuery;
import dev.amir.synapse.messaging.domain.port.in.list_messages.ListMessagesUseCase;
import dev.amir.synapse.messaging.domain.port.in.send_message.SendMessageCommand;
import dev.amir.synapse.messaging.domain.port.in.send_message.SendMessageUseCase;
import dev.amir.synapse.messaging.domain.port.out.SaveRoomPort;
import dev.amir.synapse.messaging.domain.value_object.MemberId;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.simple.JdbcClient;
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
class MessageJdbcAdapterIntegrationTest {

  @Container
  private static final PostgreSQLContainer<?> POSTGRES =
      new PostgreSQLContainer<>("postgres:16-alpine")
          .withDatabaseName("synapse_message_test")
          .withUsername("synapse")
          .withPassword("synapse");

  @Autowired private SendMessageUseCase sendMessageUseCase;
  @Autowired private ListMessagesUseCase listMessagesUseCase;
  @Autowired private SaveRoomPort saveRoomPort;
  @Autowired private JdbcClient jdbcClient;

  @DynamicPropertySource
  static void registerDatasourceProperties(DynamicPropertyRegistry registry) {
    registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
    registry.add("spring.datasource.username", POSTGRES::getUsername);
    registry.add("spring.datasource.password", POSTGRES::getPassword);
  }

  @Test
  void concurrentExactRetriesReturnOneCanonicalMessageAndAdvanceActivityOnce() throws Exception {
    var senderId = UUID.randomUUID();
    var roomId = saveGroup(senderId);
    var clientMessageId = UUID.randomUUID();
    var versionBefore = roomVersion(roomId);
    var lastActivityBefore = roomLastActivity(roomId);
    var workerCount = 4;
    var ready = new CountDownLatch(workerCount);
    var start = new CountDownLatch(1);
    var executor = Executors.newFixedThreadPool(workerCount);

    try {
      var futures = new ArrayList<java.util.concurrent.Future<UUID>>();
      for (var index = 0; index < workerCount; index++) {
        futures.add(
            executor.submit(
                () -> {
                  ready.countDown();
                  if (!start.await(10, TimeUnit.SECONDS)) {
                    throw new IllegalStateException("Concurrent retry start timed out.");
                  }
                  return send(senderId, roomId, clientMessageId, "exact retry").messageId();
                }));
      }
      assertThat(ready.await(10, TimeUnit.SECONDS)).isTrue();
      start.countDown();

      var canonicalIds = new HashSet<UUID>();
      for (var future : futures) {
        canonicalIds.add(future.get(10, TimeUnit.SECONDS));
      }

      assertThat(canonicalIds).hasSize(1);
      assertThat(messageCount(roomId)).isOne();
      assertThat(roomVersion(roomId)).isEqualTo(versionBefore + 1);
      assertThat(roomLastActivity(roomId)).isAfter(lastActivityBefore);
    } finally {
      executor.shutdownNow();
    }
  }

  @Test
  void conflictingReuseOfAClientMessageIdIsRejectedWithoutAnotherActivityUpdate() {
    var senderId = UUID.randomUUID();
    var roomId = saveGroup(senderId);
    var clientMessageId = UUID.randomUUID();
    var first = send(senderId, roomId, clientMessageId, "original");
    var versionAfterFirst = roomVersion(roomId);

    assertThat(send(senderId, roomId, clientMessageId, "original")).isEqualTo(first);
    assertThatThrownBy(() -> send(senderId, roomId, clientMessageId, "different"))
        .isInstanceOf(MessageIdempotencyConflictException.class);
    assertThat(messageCount(roomId)).isOne();
    assertThat(roomVersion(roomId)).isEqualTo(versionAfterFirst);
  }

  @Test
  void channelOwnerAndAdminCanSendButMemberCannot() {
    var ownerId = MemberId.generate();
    var adminId = MemberId.generate();
    var memberId = MemberId.generate();
    var channel = Room.createChannel(ownerId, "Announcements", null);
    channel.addMembers(Set.of(adminId, memberId));
    channel.changeMemberRole(ownerId, adminId, RoomRole.ADMIN);
    saveRoomPort.save(channel);
    var roomId = channel.getId().getValue();

    send(ownerId.getValue(), roomId, UUID.randomUUID(), "owner message");
    send(adminId.getValue(), roomId, UUID.randomUUID(), "admin message");

    assertThatThrownBy(() -> send(memberId.getValue(), roomId, UUID.randomUUID(), "member message"))
        .isInstanceOf(MessageRoomAccessDeniedException.class);
    assertThat(messageCount(roomId)).isEqualTo(2);
  }

  @Test
  void archivedMemberCanReadHistoryButCannotSendAndOutsidersCannotRead() {
    var memberId = UUID.randomUUID();
    var roomId = saveGroup(memberId);
    var saved = send(memberId, roomId, UUID.randomUUID(), "durable history");
    jdbcClient
        .sql("UPDATE rooms SET status = 'ARCHIVED', version = version + 1 WHERE id = :roomId")
        .param("roomId", roomId)
        .update();

    var result = listMessagesUseCase.handle(new ListMessagesQuery(memberId, roomId, 50, null));

    assertThat(result.items()).containsExactly(saved);
    assertThat(result.nextCursor()).isNull();
    assertThatThrownBy(() -> send(memberId, roomId, UUID.randomUUID(), "too late"))
        .isInstanceOf(MessageRoomAccessDeniedException.class);
    assertThatThrownBy(
            () ->
                listMessagesUseCase.handle(
                    new ListMessagesQuery(UUID.randomUUID(), roomId, 50, null)))
        .isInstanceOf(MessageRoomAccessDeniedException.class);
  }

  @Test
  void notActiveRoomsRejectSendAndHistory() {
    var memberId = UUID.randomUUID();
    var roomId = saveGroup(memberId);
    jdbcClient
        .sql("UPDATE rooms SET status = 'NOT_ACTIVE', version = version + 1 WHERE id = :roomId")
        .param("roomId", roomId)
        .update();

    assertThatThrownBy(() -> send(memberId, roomId, UUID.randomUUID(), "not active"))
        .isInstanceOf(MessageRoomAccessDeniedException.class);
    assertThatThrownBy(
            () -> listMessagesUseCase.handle(new ListMessagesQuery(memberId, roomId, 50, null)))
        .isInstanceOf(MessageRoomAccessDeniedException.class);
  }

  @Test
  void cursorPaginationIsStableWhenMessageTimestampsTie() {
    var memberId = UUID.randomUUID();
    var roomId = saveGroup(memberId);
    for (var index = 0; index < 5; index++) {
      send(memberId, roomId, UUID.randomUUID(), "message " + index);
    }
    var tiedTimestamp = Instant.parse("2026-08-13T12:00:00Z");
    jdbcClient
        .sql("UPDATE messages SET created_at = :createdAt WHERE room_id = :roomId")
        .param("createdAt", OffsetDateTime.ofInstant(tiedTimestamp, ZoneOffset.UTC))
        .param("roomId", roomId)
        .update();
    var expectedIds =
        jdbcClient
            .sql(
                "SELECT id FROM messages WHERE room_id = :roomId ORDER BY created_at DESC, id DESC")
            .param("roomId", roomId)
            .query(UUID.class)
            .list();

    var first = listMessagesUseCase.handle(new ListMessagesQuery(memberId, roomId, 3, null));
    var second =
        listMessagesUseCase.handle(new ListMessagesQuery(memberId, roomId, 3, first.nextCursor()));
    var actualIds =
        java.util.stream.Stream.concat(first.items().stream(), second.items().stream())
            .map(message -> message.messageId())
            .toList();

    assertThat(first.items()).hasSize(3);
    assertThat(first.nextCursor()).isNotNull();
    assertThat(second.items()).hasSize(2);
    assertThat(second.nextCursor()).isNull();
    assertThat(actualIds).containsExactlyElementsOf(expectedIds).doesNotHaveDuplicates();
  }

  private UUID saveGroup(UUID memberId) {
    var room = Room.createGroupRoom(MemberId.of(memberId), "Messages", null);
    saveRoomPort.save(room);
    return room.getId().getValue();
  }

  private dev.amir.synapse.messaging.domain.port.in.message.MessageView send(
      UUID senderId, UUID roomId, UUID clientMessageId, String text) {
    return sendMessageUseCase.handle(
        new SendMessageCommand(senderId, roomId, clientMessageId, text));
  }

  private long roomVersion(UUID roomId) {
    return jdbcClient
        .sql("SELECT version FROM rooms WHERE id = :roomId")
        .param("roomId", roomId)
        .query(Long.class)
        .single();
  }

  private Instant roomLastActivity(UUID roomId) {
    return jdbcClient
        .sql("SELECT last_messages_at FROM rooms WHERE id = :roomId")
        .param("roomId", roomId)
        .query(OffsetDateTime.class)
        .single()
        .toInstant();
  }

  private long messageCount(UUID roomId) {
    return jdbcClient
        .sql("SELECT count(*) FROM messages WHERE room_id = :roomId")
        .param("roomId", roomId)
        .query(Long.class)
        .single();
  }
}
