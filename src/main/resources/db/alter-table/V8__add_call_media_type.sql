ALTER TABLE calls
    ADD COLUMN media_type VARCHAR(10) DEFAULT 'VOICE';

UPDATE calls
SET media_type = 'VOICE'
WHERE media_type IS NULL;

ALTER TABLE calls
    ALTER COLUMN media_type SET NOT NULL,
    ALTER COLUMN media_type DROP DEFAULT,
    ADD CONSTRAINT chk_calls_media_type CHECK (media_type IN ('VOICE', 'VIDEO'));
