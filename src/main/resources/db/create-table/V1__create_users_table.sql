CREATE TABLE users
(
    id                  UUID         NOT NULL,
    google_id           VARCHAR(64)  NOT NULL,
    email               VARCHAR(255) NOT NULL,
    display_name        VARCHAR(32)  NOT NULL,
    handle              VARCHAR(32)  NOT NULL,
    profile_picture_url TEXT,
    created_at          TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    CONSTRAINT pk_users PRIMARY KEY (id),
    CONSTRAINT uq_user_google_id UNIQUE (google_id),
    CONSTRAINT uq_user_email UNIQUE (email),
    CONSTRAINT uq_user_handle UNIQUE (handle)
);

CREATE INDEX idx_user_handle_prefix ON users (handle varchar_pattern_ops);
