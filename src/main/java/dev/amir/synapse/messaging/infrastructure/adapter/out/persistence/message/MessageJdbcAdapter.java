package dev.amir.synapse.messaging.infrastructure.adapter.out.persistence.message;

import dev.amir.synapse.messaging.domain.exception.MessageIdempotencyConflictException;
import dev.amir.synapse.messaging.domain.exception.MessageRoomAccessDeniedException;
import dev.amir.synapse.messaging.domain.port.in.message.MessageView;
import dev.amir.synapse.messaging.domain.port.out.MessageHistoryPort;
import dev.amir.synapse.messaging.domain.port.out.MessageWritePort;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import org.jspecify.annotations.Nullable;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
public class MessageJdbcAdapter implements MessageWritePort, MessageHistoryPort {
  private static final int EXPECTED_UPDATED_ROOM_COUNT = 1;
  private static final String ROOM_ID_PARAMETER = "roomId";
  private static final String SENDER_ID_PARAMETER = "senderId";

  private static final String INSERT_MESSAGE_SQL =
      """
      INSERT INTO messages (id, room_id, sender_id, client_message_id, text)
      SELECT :messageId, r.id, :senderId, :clientMessageId, :text
      FROM rooms r
      JOIN room_members rm ON rm.room_id = r.id AND rm.user_id = :senderId
      WHERE r.id = :roomId
        AND r.status = 'ACTIVE'
        AND (r.room_type <> 'CHANNEL' OR rm.role IN ('OWNER', 'ADMIN'))
      ON CONFLICT (sender_id, client_message_id) DO NOTHING
      RETURNING id, room_id, sender_id, client_message_id, text, created_at
      """;

  private static final String UPDATE_ROOM_ACTIVITY_SQL =
      """
      UPDATE rooms r
      SET last_messages_at = GREATEST(r.last_messages_at, :createdAt),
          version = r.version + 1
      WHERE r.id = :roomId
        AND r.status = 'ACTIVE'
        AND EXISTS (
          SELECT 1
          FROM room_members rm
          WHERE rm.room_id = r.id
            AND rm.user_id = :senderId
            AND (r.room_type <> 'CHANNEL' OR rm.role IN ('OWNER', 'ADMIN'))
        )
      """;

  private static final String CAN_SEND_SQL =
      """
      SELECT EXISTS (
        SELECT 1
        FROM rooms r
        JOIN room_members rm ON rm.room_id = r.id AND rm.user_id = :senderId
        WHERE r.id = :roomId
          AND r.status = 'ACTIVE'
          AND (r.room_type <> 'CHANNEL' OR rm.role IN ('OWNER', 'ADMIN'))
      ) AS authorized
      """;

  private static final String FIND_BY_IDEMPOTENCY_KEY_SQL =
      """
      SELECT id, room_id, sender_id, client_message_id, text, created_at
      FROM messages
      WHERE sender_id = :senderId
        AND client_message_id = :clientMessageId
      """;

  private static final String FIND_MESSAGES_SQL =
      """
      SELECT m.id, m.room_id, m.sender_id, m.client_message_id, m.text, m.created_at
      FROM messages m
      JOIN rooms r ON r.id = m.room_id
      JOIN room_members rm ON rm.room_id = r.id AND rm.user_id = :requesterId
      WHERE m.room_id = :roomId
        AND r.status IN ('ACTIVE', 'ARCHIVED')
      ORDER BY m.created_at DESC, m.id DESC
      LIMIT :fetchSize
      """;

  private static final String FIND_MESSAGES_BEFORE_CURSOR_SQL =
      """
      SELECT m.id, m.room_id, m.sender_id, m.client_message_id, m.text, m.created_at
      FROM messages m
      JOIN rooms r ON r.id = m.room_id
      JOIN room_members rm ON rm.room_id = r.id AND rm.user_id = :requesterId
      WHERE m.room_id = :roomId
        AND r.status IN ('ACTIVE', 'ARCHIVED')
        AND (m.created_at, m.id) < (:beforeCreatedAt, :beforeMessageId)
      ORDER BY m.created_at DESC, m.id DESC
      LIMIT :fetchSize
      """;

  private static final String CAN_READ_HISTORY_SQL =
      """
      SELECT EXISTS (
        SELECT 1
        FROM rooms r
        JOIN room_members rm ON rm.room_id = r.id AND rm.user_id = :requesterId
        WHERE r.id = :roomId
          AND r.status IN ('ACTIVE', 'ARCHIVED')
      ) AS authorized
      """;

  private final JdbcClient jdbcClient;

  public MessageJdbcAdapter(JdbcClient jdbcClient) {
    this.jdbcClient = jdbcClient;
  }

  @Override
  public MessageView saveAuthorized(UUID roomId, UUID senderId, UUID clientMessageId, String text) {
    var inserted =
        jdbcClient
            .sql(INSERT_MESSAGE_SQL)
            .param("messageId", UUID.randomUUID())
            .param(ROOM_ID_PARAMETER, roomId)
            .param(SENDER_ID_PARAMETER, senderId)
            .param("clientMessageId", clientMessageId)
            .param("text", text)
            .query(MessageJdbcAdapter::mapMessage)
            .optional();

    if (inserted.isPresent()) {
      var message = inserted.orElseThrow();
      var updatedRooms =
          jdbcClient
              .sql(UPDATE_ROOM_ACTIVITY_SQL)
              .param("createdAt", toOffsetDateTime(message.createdAt()))
              .param(ROOM_ID_PARAMETER, roomId)
              .param(SENDER_ID_PARAMETER, senderId)
              .update();
      if (updatedRooms != EXPECTED_UPDATED_ROOM_COUNT) {
        throw new MessageRoomAccessDeniedException();
      }
      return message;
    }

    if (!canSend(roomId, senderId)) {
      throw new MessageRoomAccessDeniedException();
    }

    var existing =
        jdbcClient
            .sql(FIND_BY_IDEMPOTENCY_KEY_SQL)
            .param(SENDER_ID_PARAMETER, senderId)
            .param("clientMessageId", clientMessageId)
            .query(MessageJdbcAdapter::mapMessage)
            .optional()
            .orElseThrow(
                () ->
                    new IllegalStateException(
                        "The conflicting message disappeared during idempotency resolution."));

    if (!existing.roomId().equals(roomId) || !existing.text().equals(text)) {
      throw new MessageIdempotencyConflictException();
    }
    return existing;
  }

  @Override
  public List<MessageView> findAuthorized(
      UUID roomId,
      UUID requesterId,
      int fetchSize,
      @Nullable Instant beforeCreatedAt,
      @Nullable UUID beforeMessageId) {
    if ((beforeCreatedAt == null) != (beforeMessageId == null)) {
      throw new IllegalArgumentException("Both message cursor fields must be provided together.");
    }

    List<MessageView> messages;
    if (beforeCreatedAt == null) {
      messages =
          jdbcClient
              .sql(FIND_MESSAGES_SQL)
              .param(ROOM_ID_PARAMETER, roomId)
              .param("requesterId", requesterId)
              .param("fetchSize", fetchSize)
              .query(MessageJdbcAdapter::mapMessage)
              .list();
    } else {
      messages =
          jdbcClient
              .sql(FIND_MESSAGES_BEFORE_CURSOR_SQL)
              .param(ROOM_ID_PARAMETER, roomId)
              .param("requesterId", requesterId)
              .param("beforeCreatedAt", toOffsetDateTime(beforeCreatedAt))
              .param("beforeMessageId", beforeMessageId)
              .param("fetchSize", fetchSize)
              .query(MessageJdbcAdapter::mapMessage)
              .list();
    }

    if (messages.isEmpty() && !canReadHistory(roomId, requesterId)) {
      throw new MessageRoomAccessDeniedException();
    }
    return messages;
  }

  private boolean canSend(UUID roomId, UUID senderId) {
    return jdbcClient
        .sql(CAN_SEND_SQL)
        .param(ROOM_ID_PARAMETER, roomId)
        .param(SENDER_ID_PARAMETER, senderId)
        .query(MessageJdbcAdapter::mapAuthorized)
        .single();
  }

  private boolean canReadHistory(UUID roomId, UUID requesterId) {
    return jdbcClient
        .sql(CAN_READ_HISTORY_SQL)
        .param(ROOM_ID_PARAMETER, roomId)
        .param("requesterId", requesterId)
        .query(MessageJdbcAdapter::mapAuthorized)
        .single();
  }

  @SuppressWarnings("PMD.UnusedFormalParameter")
  private static MessageView mapMessage(ResultSet resultSet, int rowNumber) throws SQLException {
    return new MessageView(
        resultSet.getObject("id", UUID.class),
        resultSet.getObject("room_id", UUID.class),
        resultSet.getObject("sender_id", UUID.class),
        resultSet.getObject("client_message_id", UUID.class),
        resultSet.getString("text"),
        resultSet.getObject("created_at", OffsetDateTime.class).toInstant());
  }

  @SuppressWarnings("PMD.UnusedFormalParameter")
  private static boolean mapAuthorized(ResultSet resultSet, int rowNumber) throws SQLException {
    return resultSet.getBoolean("authorized");
  }

  private static OffsetDateTime toOffsetDateTime(Instant instant) {
    return OffsetDateTime.ofInstant(instant, ZoneOffset.UTC);
  }
}
