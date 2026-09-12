package dev.amir.synapse.messaging.infrastructure.adapter.out.persistence.message;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import dev.amir.synapse.messaging.application.model.StoredVoiceMedia;
import dev.amir.synapse.messaging.application.port.out.MessagePublicationPort;
import dev.amir.synapse.messaging.application.query.list_messages.ListMessagesHandler;
import dev.amir.synapse.messaging.application.service.VoiceMessageTransactionService;
import dev.amir.synapse.messaging.domain.enums.MessageType;
import dev.amir.synapse.messaging.domain.exception.MessageIdempotencyConflictException;
import dev.amir.synapse.messaging.domain.port.in.list_messages.ListMessagesQuery;
import dev.amir.synapse.messaging.domain.port.in.list_messages.ListMessagesUseCase;
import dev.amir.synapse.messaging.domain.port.in.send_voice_message.SendVoiceMessageCommand;
import dev.amir.synapse.messaging.domain.port.out.MessageWritePort;
import dev.amir.synapse.messaging.domain.port.out.VoiceMessageWritePort;
import dev.amir.synapse.messaging.domain.port.out.VoiceMessageWriteRequest;
import java.io.ByteArrayInputStream;
import java.util.HashSet;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jdbc.test.autoconfigure.JdbcTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@JdbcTest(
    properties = {
      "spring.jpa.hibernate.ddl-auto=validate",
      "spring.flyway.enabled=true",
      "spring.flyway.locations=classpath:db/create-table,classpath:db/alter-table"
    })
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({
  MessageJdbcAdapter.class,
  VoiceMessageJdbcAdapter.class,
  ListMessagesHandler.class,
  VoiceMessageTransactionService.class
})
@Testcontainers
class VoiceMessagePersistenceIntegrationTest {
  @Container
  private static final PostgreSQLContainer<?> POSTGRES =
      new PostgreSQLContainer<>("postgres:16-alpine")
          .withDatabaseName("synapse_voice_message_test")
          .withUsername("synapse")
          .withPassword("synapse");

  @Autowired private VoiceMessageWritePort writePort;
  @Autowired private MessageWritePort textWritePort;
  @Autowired private ListMessagesUseCase listMessagesUseCase;
  @Autowired private VoiceMessageTransactionService transactionService;
  @Autowired private JdbcClient jdbcClient;
  @MockitoBean private MessagePublicationPort publicationPort;

  @DynamicPropertySource
  static void registerDatasourceProperties(DynamicPropertyRegistry registry) {
    registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
    registry.add("spring.datasource.username", POSTGRES::getUsername);
    registry.add("spring.datasource.password", POSTGRES::getPassword);
  }

  @Test
  void persistsTypedVoiceMetadataAndResolvesIdempotency() {
    var senderId = UUID.randomUUID();
    var roomId = UUID.randomUUID();
    createGroupRoom(roomId, senderId);
    var clientMessageId = UUID.randomUUID();
    var hash = new byte[32];
    var first = request(UUID.randomUUID(), roomId, senderId, clientMessageId, hash, 12_000);

    var created = writePort.saveAuthorized(first);
    var replay =
        writePort.saveAuthorized(
            request(UUID.randomUUID(), roomId, senderId, clientMessageId, hash, 12_000));
    var history =
        listMessagesUseCase.handle(new ListMessagesQuery(senderId, roomId, 50, null)).items();

    assertThat(created.created()).isTrue();
    assertThat(replay.created()).isFalse();
    assertThat(replay.message().messageId()).isEqualTo(created.message().messageId());
    assertThat(history).containsExactly(created.message());
    assertThat(history.getFirst().type()).isEqualTo(MessageType.VOICE);
    assertThat(history.getFirst().voice())
        .satisfies(
            voice -> {
              assertThat(voice.durationMs()).isEqualTo(12_000);
              assertThat(voice.mimeType()).isEqualTo("audio/ogg");
              assertThat(voice.sizeBytes()).isEqualTo(128);
            });

    assertThatThrownBy(
            () ->
                writePort.saveAuthorized(
                    request(UUID.randomUUID(), roomId, senderId, clientMessageId, hash, 13_000)))
        .isInstanceOf(MessageIdempotencyConflictException.class);
    assertThatThrownBy(
            () -> textWritePort.saveAuthorized(roomId, senderId, clientMessageId, "not voice"))
        .isInstanceOf(MessageIdempotencyConflictException.class);
  }

  @Test
  void existingTextMessagesRemainTypedAndReadable() {
    var senderId = UUID.randomUUID();
    var roomId = UUID.randomUUID();
    createGroupRoom(roomId, senderId);

    var saved =
        textWritePort.saveAuthorized(
            roomId, senderId, UUID.randomUUID(), "text remains compatible");
    var history =
        listMessagesUseCase.handle(new ListMessagesQuery(senderId, roomId, 50, null)).items();

    assertThat(saved.type()).isEqualTo(MessageType.TEXT);
    assertThat(saved.voice()).isNull();
    assertThat(history).containsExactly(saved);
  }

  @Test
  @Transactional(propagation = Propagation.NOT_SUPPORTED)
  void concurrentExactRetriesResolveToOneCanonicalVoiceMessage() throws Exception {
    var senderId = UUID.randomUUID();
    var roomId = UUID.randomUUID();
    var clientMessageId = UUID.randomUUID();
    var hash = new byte[32];
    var command = command(senderId, roomId, clientMessageId);
    createGroupRoom(roomId, senderId);
    var workers = 4;
    var ready = new CountDownLatch(workers);
    var start = new CountDownLatch(1);
    var executor = Executors.newFixedThreadPool(workers);

    try {
      var futures = new java.util.ArrayList<java.util.concurrent.Future<UUID>>();
      for (var index = 0; index < workers; index++) {
        futures.add(
            executor.submit(
                () -> {
                  ready.countDown();
                  if (!start.await(10, TimeUnit.SECONDS)) {
                    throw new IllegalStateException("Concurrent retry start timed out.");
                  }
                  var candidateId = UUID.randomUUID();
                  return transactionService
                      .create(
                          candidateId,
                          command,
                          new StoredVoiceMedia("voice/ab/" + candidateId, "audio/ogg", 128, hash))
                      .message()
                      .messageId();
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
    } finally {
      executor.shutdownNow();
    }
  }

  private void createGroupRoom(UUID roomId, UUID senderId) {
    jdbcClient
        .sql(
            """
            INSERT INTO rooms
                (id, room_type, name, status, created_at, last_messages_at, version)
            VALUES
                (:roomId, 'GROUP', 'Voice', 'ACTIVE', clock_timestamp(), clock_timestamp(), 0)
            """)
        .param("roomId", roomId)
        .update();
    jdbcClient
        .sql(
            """
            INSERT INTO room_members (room_id, user_id, role, joined_at)
            VALUES (:roomId, :senderId, 'OWNER', clock_timestamp())
            """)
        .param("roomId", roomId)
        .param("senderId", senderId)
        .update();
  }

  private long messageCount(UUID roomId) {
    return jdbcClient
        .sql("SELECT COUNT(*) FROM messages WHERE room_id = :roomId")
        .param("roomId", roomId)
        .query(Long.class)
        .single();
  }

  private static SendVoiceMessageCommand command(UUID senderId, UUID roomId, UUID clientMessageId) {
    return new SendVoiceMessageCommand(
        senderId,
        roomId,
        clientMessageId,
        12_000,
        "audio/ogg",
        128,
        () -> new ByteArrayInputStream(new byte[128]));
  }

  private static VoiceMessageWriteRequest request(
      UUID messageId,
      UUID roomId,
      UUID senderId,
      UUID clientMessageId,
      byte[] hash,
      int durationMs) {
    return new VoiceMessageWriteRequest(
        messageId,
        roomId,
        senderId,
        clientMessageId,
        "voice/ab/" + messageId,
        "audio/ogg",
        128,
        durationMs,
        hash);
  }
}
