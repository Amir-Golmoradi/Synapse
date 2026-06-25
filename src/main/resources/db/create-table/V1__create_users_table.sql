CREATE TABLE users
(
    id                  UUID PRIMARY KEY,
    google_id           VARCHAR(64)  NOT NULL UNIQUE,
    email               VARCHAR(255) NOT NULL UNIQUE,
    first_name          VARCHAR(100) NOT NULL,
    last_name           VARCHAR(100) NOT NULL,
    profile_picture_url TEXT,
    created_at          TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);