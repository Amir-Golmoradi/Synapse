CREATE TABLE messages
(
    id         UUID PRIMARY KEY,
    room_id    UUID                     NOT NULL,
    sender_id  UUID                     NOT NULL,
    content    TEXT                     NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_messages_room FOREIGN KEY (room_id)
        REFERENCES rooms (id) ON DELETE CASCADE
);

-- ایندکس ترکیبی Keyset Pagination برای لود فوق‌العاده سریع تاریخچه چت
CREATE INDEX idx_messages_room_pagination
    ON messages (room_id, created_at DESC, id DESC);