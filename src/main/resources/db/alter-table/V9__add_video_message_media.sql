ALTER TABLE messages
    ADD COLUMN message_type VARCHAR(16) NOT NULL DEFAULT 'TEXT';

ALTER TABLE messages
    ALTER COLUMN text DROP NOT NULL;

ALTER TABLE messages
    DROP CONSTRAINT chk_messages_text_length;

ALTER TABLE messages
    ADD CONSTRAINT chk_messages_type
        CHECK (message_type IN ('TEXT', 'VIDEO')),
    ADD CONSTRAINT chk_messages_body
        CHECK (
            (message_type = 'TEXT' AND text IS NOT NULL
                AND char_length(text) BETWEEN 1 AND 4096)
            OR
            (message_type = 'VIDEO' AND text IS NULL)
        );

CREATE TABLE message_media
(
    message_id     UUID         NOT NULL,
    storage_key    VARCHAR(255) NOT NULL,
    content_type   VARCHAR(64)  NOT NULL,
    size_bytes     BIGINT       NOT NULL,
    content_sha256 BYTEA        NOT NULL,
    duration_ms    BIGINT       NOT NULL,
    width          INTEGER      NOT NULL,
    height         INTEGER      NOT NULL,
    video_codec    VARCHAR(16)  NOT NULL,
    audio_codec    VARCHAR(16),
    CONSTRAINT pk_message_media PRIMARY KEY (message_id),
    CONSTRAINT fk_message_media_message FOREIGN KEY (message_id)
        REFERENCES messages (id) ON DELETE CASCADE,
    CONSTRAINT uq_message_media_storage_key UNIQUE (storage_key),
    CONSTRAINT chk_message_media_size CHECK (size_bytes BETWEEN 1 AND 26214400),
    CONSTRAINT chk_message_media_digest CHECK (octet_length(content_sha256) = 32),
    CONSTRAINT chk_message_media_duration CHECK (duration_ms BETWEEN 1 AND 60000),
    CONSTRAINT chk_message_media_dimensions CHECK (
        width BETWEEN 1 AND 1920
        AND height BETWEEN 1 AND 1920
        AND width::BIGINT * height::BIGINT <= 2073600
    ),
    CONSTRAINT chk_message_media_format CHECK (
        (content_type = 'video/webm'
            AND video_codec IN ('VP8', 'VP9')
            AND (audio_codec IS NULL OR audio_codec = 'OPUS'))
        OR
        (content_type = 'video/mp4'
            AND video_codec = 'H264'
            AND (audio_codec IS NULL OR audio_codec = 'AAC'))
    )
);
