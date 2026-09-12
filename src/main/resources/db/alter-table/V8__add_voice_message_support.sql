ALTER TABLE messages
    ADD COLUMN message_type VARCHAR(16) NOT NULL DEFAULT 'TEXT';

ALTER TABLE messages
    ALTER COLUMN text DROP NOT NULL;

ALTER TABLE messages
    DROP CONSTRAINT chk_messages_text_length;

ALTER TABLE messages
    ADD CONSTRAINT chk_messages_type
        CHECK (message_type IN ('TEXT', 'VOICE')),
    ADD CONSTRAINT chk_messages_content
        CHECK (
            (message_type = 'TEXT' AND text IS NOT NULL AND char_length(text) BETWEEN 1 AND 4096)
            OR (message_type = 'VOICE' AND text IS NULL)
        );

CREATE TABLE voice_message_media
(
    message_id  UUID PRIMARY KEY,
    storage_key VARCHAR(255) NOT NULL UNIQUE,
    mime_type   VARCHAR(64)  NOT NULL,
    size_bytes  BIGINT       NOT NULL,
    duration_ms INTEGER      NOT NULL,
    sha256      BYTEA        NOT NULL,
    CONSTRAINT fk_voice_message_media_message FOREIGN KEY (message_id)
        REFERENCES messages (id) ON DELETE CASCADE,
    CONSTRAINT chk_voice_message_media_mime
        CHECK (mime_type IN ('audio/webm', 'audio/ogg', 'audio/mp4')),
    CONSTRAINT chk_voice_message_media_size
        CHECK (size_bytes BETWEEN 1 AND 16777216),
    CONSTRAINT chk_voice_message_media_duration
        CHECK (duration_ms BETWEEN 1 AND 300000),
    CONSTRAINT chk_voice_message_media_sha256
        CHECK (octet_length(sha256) = 32),
    CONSTRAINT chk_voice_message_media_storage_key
        CHECK (char_length(storage_key) BETWEEN 1 AND 255)
);
