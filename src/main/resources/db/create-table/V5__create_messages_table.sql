CREATE TABLE messages
(
    id                UUID PRIMARY KEY,
    room_id           UUID        NOT NULL,
    sender_id         UUID        NOT NULL,
    client_message_id UUID        NOT NULL,
    text              TEXT        NOT NULL,
    created_at        TIMESTAMPTZ NOT NULL DEFAULT clock_timestamp(),
    CONSTRAINT fk_messages_room FOREIGN KEY (room_id)
        REFERENCES rooms (id) ON DELETE CASCADE,
    CONSTRAINT uq_messages_sender_client_message UNIQUE (sender_id, client_message_id),
    CONSTRAINT chk_messages_text_length CHECK (char_length(text) BETWEEN 1 AND 4096)
);

CREATE INDEX idx_messages_room_created_at_id_desc
    ON messages (room_id, created_at DESC, id DESC);
