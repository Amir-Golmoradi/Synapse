CREATE TABLE rooms
(
    id               UUID PRIMARY KEY,
    room_type        VARCHAR(20)  NOT NULL,
    name             VARCHAR(100),              -- NULL allowed for DIRECT rooms
    avatar_url       VARCHAR(2048),
    status           VARCHAR(20)  NOT NULL,
    created_at       TIMESTAMPTZ  NOT NULL,
    last_messages_at TIMESTAMPTZ  NOT NULL,
    version          BIGINT       NOT NULL,
    CONSTRAINT chk_rooms_room_type CHECK (room_type IN ('DIRECT', 'GROUP', 'CHANNEL')),
    CONSTRAINT chk_rooms_status CHECK (status IN ('ACTIVE', 'NOT_ACTIVE', 'ARCHIVED')),
    CONSTRAINT chk_rooms_name_required CHECK (room_type = 'DIRECT' OR name IS NOT NULL)
);

CREATE INDEX idx_rooms_status ON rooms (status);
CREATE INDEX idx_rooms_type ON rooms (room_type);
CREATE INDEX idx_rooms_last_messages_at ON rooms (last_messages_at);

CREATE TABLE room_members
(
    room_id UUID NOT NULL,
    user_id UUID NOT NULL,
    PRIMARY KEY (room_id, user_id),
    CONSTRAINT fk_room_members_room FOREIGN KEY (room_id)
        REFERENCES rooms (id) ON DELETE CASCADE
);

CREATE INDEX idx_room_members_user_id ON room_members (user_id);