CREATE TABLE message_receipts
(
    message_id   UUID NOT NULL,
    user_id      UUID NOT NULL,
    delivered_at TIMESTAMP WITH TIME ZONE,
    read_at      TIMESTAMP WITH TIME ZONE,
    PRIMARY KEY (message_id, user_id),
    CONSTRAINT fk_receipts_message FOREIGN KEY (message_id)
        REFERENCES messages (id) ON DELETE CASCADE,
    CONSTRAINT fk_receipts_user FOREIGN KEY (user_id)
        REFERENCES users (id) ON DELETE CASCADE
);

CREATE INDEX idx_receipts_user ON message_receipts (user_id, read_at);


