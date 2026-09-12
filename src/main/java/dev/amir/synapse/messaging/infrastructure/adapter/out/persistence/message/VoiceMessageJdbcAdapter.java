package dev.amir.synapse.messaging.infrastructure.adapter.out.persistence.message;

import dev.amir.synapse.messaging.domain.enums.MessageType;
import dev.amir.synapse.messaging.domain.exception.MessageIdempotencyConflictException;
import dev.amir.synapse.messaging.domain.exception.MessageRoomAccessDeniedException;
import dev.amir.synapse.messaging.domain.port.in.message.MessageView;
import dev.amir.synapse.messaging.domain.port.out.MessageSendAuthorizationPort;
import dev.amir.synapse.messaging.domain.port.out.VoiceMediaRecord;
import dev.amir.synapse.messaging.domain.port.out.VoiceMessageMediaPort;
import dev.amir.synapse.messaging.domain.port.out.VoiceMessageWritePort;
import dev.amir.synapse.messaging.domain.port.out.VoiceMessageWriteRequest;
import dev.amir.synapse.messaging.domain.port.out.VoiceMessageWriteResult;
import dev.amir.synapse.messaging.domain.value_object.VoiceMessageMetadata;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
public class VoiceMessageJdbcAdapter implements VoiceMessageWritePort, VoiceMessageMediaPort {
  private static final int EXPECTED_ROW_COUNT = 1;
  private static final String INSERT_MESSAGE_SQL =
      """
      INSERT INTO messages
          (id, room_id, sender_id, client_message_id, message_type, text)
      SELECT :messageId, r.id, :senderId, :clientMessageId, 'VOICE', NULL
      FROM rooms r
      JOIN room_members rm ON rm.room_id = r.id AND rm.user_id = :senderId
      WHERE r.id = :roomId
        AND r.status = 'ACTIVE'
        AND (r.room_type <> 'CHANNEL' OR rm.role IN ('OWNER', 'ADMIN'))
      ON CONFLICT (sender_id, client_message_id) DO NOTHING
      RETURNING created_at
      """;
  private static final String INSERT_MEDIA_SQL =
      """
      INSERT INTO voice_message_media
          (message_id, storage_key, mime_type, size_bytes, duration_ms, sha256)
      VALUES
          (:messageId, :storageKey, :mimeType, :sizeBytes, :durationMs, :sha256)
      """;
  private static final String UPDATE_ROOM_ACTIVITY_SQL =
      """
      UPDATE rooms
      SET last_messages_at = GREATEST(last_messages_at, :createdAt), version = version + 1
      WHERE id = :roomId
      """;
  private static final String FIND_IDEMPOTENT_SQL =
      """
      SELECT m.id, m.room_id, m.sender_id, m.client_message_id, m.message_type, m.text,
             m.created_at, vm.storage_key, vm.mime_type, vm.size_bytes, vm.duration_ms, vm.sha256
      FROM messages m
      LEFT JOIN voice_message_media vm ON vm.message_id = m.id
      WHERE m.sender_id = :senderId AND m.client_message_id = :clientMessageId
      """;
  private static final String FIND_AUTHORIZED_MEDIA_SQL =
      """
      SELECT m.id AS message_id, m.room_id, vm.storage_key, vm.mime_type, vm.size_bytes,
             vm.duration_ms, m.created_at
      FROM messages m
      JOIN voice_message_media vm ON vm.message_id = m.id
      JOIN rooms r ON r.id = m.room_id
      JOIN room_members rm ON rm.room_id = r.id AND rm.user_id = :requesterId
      WHERE m.id = :messageId
        AND m.room_id = :roomId
        AND m.message_type = 'VOICE'
        AND r.status IN ('ACTIVE', 'ARCHIVED')
      """;

  private final JdbcClient jdbcClient;
  private final MessageSendAuthorizationPort authorizationPort;

  public VoiceMessageJdbcAdapter(
      JdbcClient jdbcClient, MessageSendAuthorizationPort authorizationPort) {
    this.jdbcClient = jdbcClient;
    this.authorizationPort = authorizationPort;
  }

  @Override
  public VoiceMessageWriteResult saveAuthorized(VoiceMessageWriteRequest request) {
    var createdAt =
        jdbcClient
            .sql(INSERT_MESSAGE_SQL)
            .param("messageId", request.messageId())
            .param("roomId", request.roomId())
            .param("senderId", request.senderId())
            .param("clientMessageId", request.clientMessageId())
            .query(OffsetDateTime.class)
            .optional();
    if (createdAt.isPresent()) {
      insertMedia(request);
      updateRoomActivity(request.roomId(), createdAt.orElseThrow());
      return new VoiceMessageWriteResult(view(request, createdAt.orElseThrow()), true);
    }

    if (!authorizationPort.canSend(request.roomId(), request.senderId())) {
      throw new MessageRoomAccessDeniedException();
    }
    var existing =
        jdbcClient
            .sql(FIND_IDEMPOTENT_SQL)
            .param("senderId", request.senderId())
            .param("clientMessageId", request.clientMessageId())
            .query(VoiceMessageJdbcAdapter::mapExisting)
            .optional()
            .orElseThrow(
                () ->
                    new IllegalStateException(
                        "The conflicting message disappeared during idempotency resolution."));
    if (!existing.matches(request)) {
      throw new MessageIdempotencyConflictException();
    }
    return new VoiceMessageWriteResult(existing.toView(), false);
  }

  @Override
  public Optional<VoiceMediaRecord> findAuthorized(UUID roomId, UUID messageId, UUID requesterId) {
    return jdbcClient
        .sql(FIND_AUTHORIZED_MEDIA_SQL)
        .param("roomId", roomId)
        .param("messageId", messageId)
        .param("requesterId", requesterId)
        .query(VoiceMessageJdbcAdapter::mapMedia)
        .optional();
  }

  @Override
  public boolean isStorageKeyReferenced(String storageKey) {
    return jdbcClient
        .sql("SELECT EXISTS (SELECT 1 FROM voice_message_media WHERE storage_key = :storageKey)")
        .param("storageKey", storageKey)
        .query(Boolean.class)
        .single();
  }

  private void insertMedia(VoiceMessageWriteRequest request) {
    var inserted =
        jdbcClient
            .sql(INSERT_MEDIA_SQL)
            .param("messageId", request.messageId())
            .param("storageKey", request.storageKey())
            .param("mimeType", request.mimeType())
            .param("sizeBytes", request.sizeBytes())
            .param("durationMs", request.durationMs())
            .param("sha256", request.sha256())
            .update();
    if (inserted != EXPECTED_ROW_COUNT) {
      throw new IllegalStateException("Voice message metadata was not inserted.");
    }
  }

  private void updateRoomActivity(UUID roomId, OffsetDateTime createdAt) {
    var updated =
        jdbcClient
            .sql(UPDATE_ROOM_ACTIVITY_SQL)
            .param("roomId", roomId)
            .param("createdAt", createdAt)
            .update();
    if (updated != EXPECTED_ROW_COUNT) {
      throw new MessageRoomAccessDeniedException();
    }
  }

  private static MessageView view(VoiceMessageWriteRequest request, OffsetDateTime createdAt) {
    return new MessageView(
        request.messageId(),
        request.roomId(),
        request.senderId(),
        request.clientMessageId(),
        MessageType.VOICE,
        null,
        new VoiceMessageMetadata(request.durationMs(), request.mimeType(), request.sizeBytes()),
        createdAt.toInstant());
  }

  @SuppressWarnings("PMD.UnusedFormalParameter")
  private static ExistingVoiceMessage mapExisting(ResultSet row, int rowNumber)
      throws SQLException {
    return new ExistingVoiceMessage(
        row.getObject("id", UUID.class),
        row.getObject("room_id", UUID.class),
        row.getObject("sender_id", UUID.class),
        row.getObject("client_message_id", UUID.class),
        MessageType.valueOf(row.getString("message_type")),
        row.getString("mime_type"),
        row.getLong("size_bytes"),
        row.getInt("duration_ms"),
        row.getBytes("sha256"),
        row.getObject("created_at", OffsetDateTime.class));
  }

  @SuppressWarnings("PMD.UnusedFormalParameter")
  private static VoiceMediaRecord mapMedia(ResultSet row, int rowNumber) throws SQLException {
    return new VoiceMediaRecord(
        row.getObject("message_id", UUID.class),
        row.getObject("room_id", UUID.class),
        row.getString("storage_key"),
        row.getString("mime_type"),
        row.getLong("size_bytes"),
        row.getInt("duration_ms"),
        row.getObject("created_at", OffsetDateTime.class).toInstant());
  }

  private record ExistingVoiceMessage(
      UUID messageId,
      UUID roomId,
      UUID senderId,
      UUID clientMessageId,
      MessageType type,
      String mimeType,
      long sizeBytes,
      int durationMs,
      byte[] sha256,
      OffsetDateTime createdAt) {
    boolean matches(VoiceMessageWriteRequest request) {
      return type == MessageType.VOICE
          && roomId.equals(request.roomId())
          && mimeType != null
          && mimeType.equals(request.mimeType())
          && sizeBytes == request.sizeBytes()
          && durationMs == request.durationMs()
          && Arrays.equals(sha256, request.sha256());
    }

    MessageView toView() {
      return new MessageView(
          messageId,
          roomId,
          senderId,
          clientMessageId,
          MessageType.VOICE,
          null,
          new VoiceMessageMetadata(durationMs, mimeType, sizeBytes),
          createdAt.toInstant());
    }
  }
}
