CREATE INDEX idx_room_members_user_id_room_id
    ON room_members (user_id, room_id);

CREATE INDEX idx_rooms_status_last_messages_at_desc
    ON rooms (status, last_messages_at DESC);

CREATE INDEX idx_rooms_status_type_last_messages_at_desc
    ON rooms (status, room_type, last_messages_at DESC);
