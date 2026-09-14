package dev.amir.synapse.messaging.infrastructure.adapter.out.persistence.message;

import dev.amir.synapse.messaging.domain.enums.MessageType;
import dev.amir.synapse.messaging.domain.exception.MessageIdempotencyConflictException;
import dev.amir.synapse.messaging.domain.exception.MessageRoomAccessDeniedException;
import dev.amir.synapse.messaging.domain.port.in.message.MessageView;
import dev.amir.synapse.messaging.domain.port.out.MessageAuthorizationPort;
import dev.amir.synapse.messaging.domain.port.out.MessageHistoryPort;
import dev.amir.synapse.messaging.domain.port.out.MessageMediaReadPort;
import dev.amir.synapse.messaging.domain.port.out.MessagePersistenceResult;
import dev.amir.synapse.messaging.domain.port.out.MessageWritePort;
import dev.amir.synapse.messaging.domain.value_object.VideoMetadata;
import dev.amir.synapse.messaging.domain.value_object.VideoMetadata.AudioCodec;
import dev.amir.synapse.messaging.domain.value_object.VideoMetadata.VideoCodec;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.jspecify.annotations.Nullable;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
@SuppressWarnings("PMD.UnusedFormalParameter")
public class MessageJdbcAdapter
    implements MessageWritePort,
        MessageHistoryPort,
        MessageAuthorizationPort,
        MessageMediaReadPort {
  private static final int EXPECTED_UPDATED_ROOM_COUNT = 1;
  private static final String ROOM_ID_PARAMETER = "roomId";
  private static final String SENDER_ID_PARAMETER = "senderId";
  private static final String MESSAGE_ID_PARAMETER = "messageId";
  private static final String MESSAGE_COLUMNS =
      "m.id, m.room_id, m.sender_id, m.client_message_id, m.message_type, m.text,"
          + " m.created_at, mm.storage_key, mm.content_type, mm.size_bytes,"
          + " mm.content_sha256, mm.duration_ms, mm.width, mm.height, mm.video_codec,"
          + " mm.audio_codec";

  private static final String INSERT_TEXT_SQL =
      """
      INSERT INTO messages (id, room_id, sender_id, client_message_id, message_type, text)
      SELECT :messageId, r.id, :senderId, :clientMessageId, 'TEXT', :text
      FROM rooms r
      JOIN room_members rm ON rm.room_id = r.id AND rm.user_id = :senderId
      WHERE r.id = :roomId
        AND r.status = 'ACTIVE'
        AND (r.room_type <> 'CHANNEL' OR rm.role IN ('OWNER', 'ADMIN'))
      ON CONFLICT (sender_id, client_message_id) DO NOTHING
      RETURNING id, room_id, sender_id, client_message_id, message_type, text, created_at
      """;

  private static final String INSERT_VIDEO_MESSAGE_SQL =
      """
      INSERT INTO messages (id, room_id, sender_id, client_message_id, message_type, text)
      SELECT :messageId, r.id, :senderId, :clientMessageId, 'VIDEO', NULL
      FROM rooms r
      JOIN room_members rm ON rm.room_id = r.id AND rm.user_id = :senderId
      WHERE r.id = :roomId
        AND r.status = 'ACTIVE'
        AND (r.room_type <> 'CHANNEL' OR rm.role IN ('OWNER', 'ADMIN'))
      ON CONFLICT (sender_id, client_message_id) DO NOTHING
      RETURNING id, room_id, sender_id, client_message_id, message_type, text, created_at
      """;

  private static final String INSERT_MEDIA_SQL =
      """
      INSERT INTO message_media (
        message_id, storage_key, content_type, size_bytes, content_sha256,
        duration_ms, width, height, video_codec, audio_codec)
      VALUES (
        :messageId, :storageKey, :contentType, :sizeBytes, :contentSha256,
        :durationMs, :width, :height, :videoCodec, :audioCodec)
      """;

  private static final String UPDATE_ROOM_ACTIVITY_SQL =
      """
      UPDATE rooms r
      SET last_messages_at = GREATEST(r.last_messages_at, :createdAt),
          version = r.version + 1
      WHERE r.id = :roomId
        AND r.status = 'ACTIVE'
        AND EXISTS (
          SELECT 1 FROM room_members rm
          WHERE rm.room_id = r.id AND rm.user_id = :senderId
            AND (r.room_type <> 'CHANNEL' OR rm.role IN ('OWNER', 'ADMIN')))
      """;

  private static final String CAN_SEND_SQL =
      """
      SELECT EXISTS (
        SELECT 1 FROM rooms r
        JOIN room_members rm ON rm.room_id = r.id AND rm.user_id = :senderId
        WHERE r.id = :roomId AND r.status = 'ACTIVE'
          AND (r.room_type <> 'CHANNEL' OR rm.role IN ('OWNER', 'ADMIN')))
        AS authorized
      """;

  private static final String FIND_BY_IDEMPOTENCY_KEY_SQL =
      "SELECT "
          + MESSAGE_COLUMNS
          + " FROM messages m LEFT JOIN message_media mm"
          + " ON mm.message_id = m.id WHERE m.sender_id = :senderId"
          + " AND m.client_message_id = :clientMessageId";

  private static final String FIND_MESSAGES_SQL =
      "SELECT "
          + MESSAGE_COLUMNS
          + " FROM messages m"
          + " LEFT JOIN message_media mm ON mm.message_id = m.id"
          + " JOIN rooms r ON r.id = m.room_id"
          + " JOIN room_members rm ON rm.room_id = r.id AND rm.user_id = :requesterId"
          + " WHERE m.room_id = :roomId AND r.status IN ('ACTIVE', 'ARCHIVED')"
          + " ORDER BY m.created_at DESC, m.id DESC LIMIT :fetchSize";

  private static final String FIND_MESSAGES_BEFORE_CURSOR_SQL =
      "SELECT "
          + MESSAGE_COLUMNS
          + " FROM messages m"
          + " LEFT JOIN message_media mm ON mm.message_id = m.id"
          + " JOIN rooms r ON r.id = m.room_id"
          + " JOIN room_members rm ON rm.room_id = r.id AND rm.user_id = :requesterId"
          + " WHERE m.room_id = :roomId AND r.status IN ('ACTIVE', 'ARCHIVED')"
          + " AND (m.created_at, m.id) < (:beforeCreatedAt, :beforeMessageId)"
          + " ORDER BY m.created_at DESC, m.id DESC LIMIT :fetchSize";

  private static final String CAN_READ_HISTORY_SQL =
      """
      SELECT EXISTS (
        SELECT 1 FROM rooms r
        JOIN room_members rm ON rm.room_id = r.id AND rm.user_id = :requesterId
        WHERE r.id = :roomId AND r.status IN ('ACTIVE', 'ARCHIVED')) AS authorized
      """;

  private static final String FIND_AUTHORIZED_MEDIA_SQL =
      """
      SELECT m.id, mm.storage_key, mm.content_type, mm.size_bytes, mm.content_sha256
      FROM messages m
      JOIN message_media mm ON mm.message_id = m.id
      JOIN rooms r ON r.id = m.room_id
      JOIN room_members rm ON rm.room_id = r.id AND rm.user_id = :requesterId
      WHERE m.id = :messageId AND m.message_type = 'VIDEO'
        AND r.status IN ('ACTIVE', 'ARCHIVED')
      """;

  private final JdbcClient jdbcClient;

  public MessageJdbcAdapter(JdbcClient jdbcClient) {
    this.jdbcClient = jdbcClient;
  }

  @Override
  public MessageView saveAuthorized(UUID roomId, UUID senderId, UUID clientMessageId, String text) {
    var inserted =
        jdbcClient
            .sql(INSERT_TEXT_SQL)
            .param(MESSAGE_ID_PARAMETER, UUID.randomUUID())
            .param(ROOM_ID_PARAMETER, roomId)
            .param(SENDER_ID_PARAMETER, senderId)
            .param("clientMessageId", clientMessageId)
            .param("text", text)
            .query(MessageJdbcAdapter::mapTextInsert)
            .optional();
    if (inserted.isPresent()) {
      var message = inserted.orElseThrow();
      updateActivity(message);
      return message;
    }
    requireCanSend(roomId, senderId);
    var existing = findByIdempotencyKey(senderId, clientMessageId);
    if (existing.view().type() != MessageType.TEXT
        || !existing.view().roomId().equals(roomId)
        || !existing.view().text().equals(text)) {
      throw new MessageIdempotencyConflictException();
    }
    return existing.view();
  }

  @Override
  public MessagePersistenceResult saveVideoAuthorized(
      UUID roomId,
      UUID senderId,
      UUID clientMessageId,
      String storageKey,
      byte[] contentSha256,
      VideoMetadata metadata) {
    var inserted =
        jdbcClient
            .sql(INSERT_VIDEO_MESSAGE_SQL)
            .param(MESSAGE_ID_PARAMETER, UUID.randomUUID())
            .param(ROOM_ID_PARAMETER, roomId)
            .param(SENDER_ID_PARAMETER, senderId)
            .param("clientMessageId", clientMessageId)
            .query(MessageJdbcAdapter::mapInsertedMessage)
            .optional();
    if (inserted.isPresent()) {
      var base = inserted.orElseThrow();
      insertMedia(base.messageId(), storageKey, contentSha256, metadata);
      var message =
          new MessageView(
              base.messageId(),
              base.roomId(),
              base.senderId(),
              base.clientMessageId(),
              MessageType.VIDEO,
              null,
              metadata,
              base.createdAt());
      updateActivity(message);
      return new MessagePersistenceResult(message, true);
    }
    requireCanSend(roomId, senderId);
    var existing = findByIdempotencyKey(senderId, clientMessageId);
    if (existing.view().type() != MessageType.VIDEO
        || !existing.view().roomId().equals(roomId)
        || !Arrays.equals(existing.contentSha256(), contentSha256)) {
      throw new MessageIdempotencyConflictException();
    }
    return new MessagePersistenceResult(existing.view(), false);
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
    var query =
        jdbcClient
            .sql(beforeCreatedAt == null ? FIND_MESSAGES_SQL : FIND_MESSAGES_BEFORE_CURSOR_SQL)
            .param(ROOM_ID_PARAMETER, roomId)
            .param("requesterId", requesterId)
            .param("fetchSize", fetchSize);
    if (beforeCreatedAt != null) {
      query =
          query
              .param("beforeCreatedAt", toOffsetDateTime(beforeCreatedAt))
              .param("beforeMessageId", beforeMessageId);
    }
    var messages = query.query(MessageJdbcAdapter::mapMessage).list();
    if (messages.isEmpty() && !canReadHistory(roomId, requesterId)) {
      throw new MessageRoomAccessDeniedException();
    }
    return messages;
  }

  @Override
  public boolean canSend(UUID roomId, UUID senderId) {
    return jdbcClient
        .sql(CAN_SEND_SQL)
        .param(ROOM_ID_PARAMETER, roomId)
        .param(SENDER_ID_PARAMETER, senderId)
        .query(MessageJdbcAdapter::mapAuthorized)
        .single();
  }

  @Override
  public Optional<MessageMediaDescriptor> findAuthorized(UUID messageId, UUID requesterId) {
    return jdbcClient
        .sql(FIND_AUTHORIZED_MEDIA_SQL)
        .param(MESSAGE_ID_PARAMETER, messageId)
        .param("requesterId", requesterId)
        .query(
            (resultSet, rowNumber) ->
                new MessageMediaDescriptor(
                    resultSet.getObject("id", UUID.class),
                    resultSet.getString("storage_key"),
                    resultSet.getString("content_type"),
                    resultSet.getLong("size_bytes"),
                    resultSet.getBytes("content_sha256")))
        .optional();
  }

  @Override
  public boolean isStorageKeyReferenced(String storageKey) {
    return jdbcClient
        .sql("SELECT EXISTS (SELECT 1 FROM message_media WHERE storage_key = :key)")
        .param("key", storageKey)
        .query(Boolean.class)
        .single();
  }

  private void insertMedia(
      UUID messageId, String storageKey, byte[] contentSha256, VideoMetadata metadata) {
    var statement =
        jdbcClient
            .sql(INSERT_MEDIA_SQL)
            .param(MESSAGE_ID_PARAMETER, messageId)
            .param("storageKey", storageKey)
            .param("contentType", metadata.contentType())
            .param("sizeBytes", metadata.sizeBytes())
            .param("contentSha256", contentSha256)
            .param("durationMs", metadata.durationMs())
            .param("width", metadata.width())
            .param("height", metadata.height())
            .param("videoCodec", metadata.videoCodec().name());
    if (metadata.audioCodec() == null) {
      statement.param("audioCodec", null, Types.VARCHAR).update();
    } else {
      statement.param("audioCodec", metadata.audioCodec().name()).update();
    }
  }

  private void updateActivity(MessageView message) {
    var updated =
        jdbcClient
            .sql(UPDATE_ROOM_ACTIVITY_SQL)
            .param("createdAt", toOffsetDateTime(message.createdAt()))
            .param(ROOM_ID_PARAMETER, message.roomId())
            .param(SENDER_ID_PARAMETER, message.senderId())
            .update();
    if (updated != EXPECTED_UPDATED_ROOM_COUNT) {
      throw new MessageRoomAccessDeniedException();
    }
  }

  private void requireCanSend(UUID roomId, UUID senderId) {
    if (!canSend(roomId, senderId)) {
      throw new MessageRoomAccessDeniedException();
    }
  }

  private ExistingMessage findByIdempotencyKey(UUID senderId, UUID clientMessageId) {
    return jdbcClient
        .sql(FIND_BY_IDEMPOTENCY_KEY_SQL)
        .param(SENDER_ID_PARAMETER, senderId)
        .param("clientMessageId", clientMessageId)
        .query(MessageJdbcAdapter::mapExistingMessage)
        .optional()
        .orElseThrow(
            () ->
                new IllegalStateException(
                    "The conflicting message disappeared during idempotency resolution."));
  }

  private boolean canReadHistory(UUID roomId, UUID requesterId) {
    return jdbcClient
        .sql(CAN_READ_HISTORY_SQL)
        .param(ROOM_ID_PARAMETER, roomId)
        .param("requesterId", requesterId)
        .query(MessageJdbcAdapter::mapAuthorized)
        .single();
  }

  private static MessageView mapTextInsert(ResultSet resultSet, int rowNumber) throws SQLException {
    return new MessageView(
        resultSet.getObject("id", UUID.class),
        resultSet.getObject("room_id", UUID.class),
        resultSet.getObject("sender_id", UUID.class),
        resultSet.getObject("client_message_id", UUID.class),
        MessageType.TEXT,
        resultSet.getString("text"),
        null,
        resultSet.getObject("created_at", OffsetDateTime.class).toInstant());
  }

  private static InsertedMessage mapInsertedMessage(ResultSet resultSet, int rowNumber)
      throws SQLException {
    return new InsertedMessage(
        resultSet.getObject("id", UUID.class),
        resultSet.getObject("room_id", UUID.class),
        resultSet.getObject("sender_id", UUID.class),
        resultSet.getObject("client_message_id", UUID.class),
        resultSet.getObject("created_at", OffsetDateTime.class).toInstant());
  }

  private static MessageView mapMessage(ResultSet resultSet, int rowNumber) throws SQLException {
    return mapExistingMessage(resultSet, rowNumber).view();
  }

  private static ExistingMessage mapExistingMessage(ResultSet resultSet, int rowNumber)
      throws SQLException {
    var type = MessageType.valueOf(resultSet.getString("message_type"));
    VideoMetadata media = null;
    if (type == MessageType.VIDEO) {
      var audio = resultSet.getString("audio_codec");
      media =
          new VideoMetadata(
              resultSet.getString("content_type"),
              resultSet.getLong("size_bytes"),
              resultSet.getLong("duration_ms"),
              resultSet.getInt("width"),
              resultSet.getInt("height"),
              VideoCodec.valueOf(resultSet.getString("video_codec")),
              audio == null ? null : AudioCodec.valueOf(audio));
    }
    var view =
        new MessageView(
            resultSet.getObject("id", UUID.class),
            resultSet.getObject("room_id", UUID.class),
            resultSet.getObject("sender_id", UUID.class),
            resultSet.getObject("client_message_id", UUID.class),
            type,
            resultSet.getString("text"),
            media,
            resultSet.getObject("created_at", OffsetDateTime.class).toInstant());
    return new ExistingMessage(view, resultSet.getBytes("content_sha256"));
  }

  private static boolean mapAuthorized(ResultSet resultSet, int rowNumber) throws SQLException {
    return resultSet.getBoolean("authorized");
  }

  private static OffsetDateTime toOffsetDateTime(Instant instant) {
    return OffsetDateTime.ofInstant(instant, ZoneOffset.UTC);
  }

  private record ExistingMessage(MessageView view, byte @Nullable [] contentSha256) {}

  private record InsertedMessage(
      UUID messageId, UUID roomId, UUID senderId, UUID clientMessageId, Instant createdAt) {}
}
