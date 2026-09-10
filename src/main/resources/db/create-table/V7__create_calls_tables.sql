CREATE TABLE calls
(
    id                        UUID         NOT NULL,
    caller_id                 UUID         NOT NULL,
    callee_id                 UUID         NOT NULL,
    client_request_id         UUID         NOT NULL,
    start_request_fingerprint VARCHAR(64)  NOT NULL,
    status                    VARCHAR(20)  NOT NULL,
    created_at                TIMESTAMPTZ  NOT NULL,
    updated_at                TIMESTAMPTZ  NOT NULL,
    accepted_at               TIMESTAMPTZ,
    connected_at              TIMESTAMPTZ,
    ended_at                  TIMESTAMPTZ,
    deadline_at               TIMESTAMPTZ,
    termination_reason        VARCHAR(32),
    terminated_by             UUID,
    version                   BIGINT       NOT NULL,
    CONSTRAINT pk_calls PRIMARY KEY (id),
    CONSTRAINT uq_calls_caller_request UNIQUE (caller_id, client_request_id),
    CONSTRAINT chk_calls_distinct_participants CHECK (caller_id <> callee_id),
    CONSTRAINT chk_calls_status CHECK (status IN
        ('RINGING', 'CONNECTING', 'ACTIVE', 'RECOVERING',
         'REJECTED', 'CANCELLED', 'MISSED', 'ENDED', 'FAILED')),
    CONSTRAINT chk_calls_reason CHECK (termination_reason IS NULL OR termination_reason IN
        ('REJECTED', 'CALLER_CANCELLED', 'NO_ANSWER', 'HANGUP', 'MEDIA_ERROR',
         'CONNECTION_TIMEOUT', 'PARTICIPANT_UNREACHABLE')),
    CONSTRAINT chk_calls_terminal_shape CHECK (
        (status IN ('REJECTED', 'CANCELLED', 'MISSED', 'ENDED', 'FAILED')
            AND ended_at IS NOT NULL AND termination_reason IS NOT NULL AND deadline_at IS NULL)
        OR
        (status IN ('RINGING', 'CONNECTING', 'ACTIVE', 'RECOVERING')
            AND ended_at IS NULL AND termination_reason IS NULL)
    ),
    CONSTRAINT chk_calls_deadline_shape CHECK (
        (status IN ('RINGING', 'CONNECTING', 'RECOVERING') AND deadline_at IS NOT NULL)
        OR
        (status NOT IN ('RINGING', 'CONNECTING', 'RECOVERING') AND deadline_at IS NULL)
    ),
    CONSTRAINT chk_calls_acceptance_shape CHECK (
        (status = 'RINGING' AND accepted_at IS NULL)
        OR
        (status IN ('CONNECTING', 'ACTIVE', 'RECOVERING', 'ENDED', 'FAILED')
            AND accepted_at IS NOT NULL)
        OR
        (status IN ('REJECTED', 'CANCELLED', 'MISSED') AND accepted_at IS NULL)
    ),
    CONSTRAINT chk_calls_connected_shape CHECK (
        status NOT IN ('ACTIVE', 'RECOVERING') OR connected_at IS NOT NULL
    ),
    CONSTRAINT chk_calls_terminated_by CHECK (
        terminated_by IS NULL OR terminated_by IN (caller_id, callee_id)
    ),
    CONSTRAINT chk_calls_timestamp_order CHECK (
        updated_at >= created_at
        AND (accepted_at IS NULL OR accepted_at >= created_at)
        AND (connected_at IS NULL OR connected_at >= accepted_at)
        AND (ended_at IS NULL OR ended_at >= created_at)
    )
);

CREATE INDEX idx_calls_caller_created_desc ON calls (caller_id, created_at DESC, id DESC);
CREATE INDEX idx_calls_callee_created_desc ON calls (callee_id, created_at DESC, id DESC);
CREATE INDEX idx_calls_due ON calls (deadline_at)
    WHERE status IN ('RINGING', 'CONNECTING', 'RECOVERING');

CREATE TABLE call_runtime
(
    call_id                    UUID        NOT NULL,
    caller_client_instance_id  UUID        NOT NULL,
    callee_client_instance_id  UUID,
    generation                 INTEGER     NOT NULL,
    caller_ready               BOOLEAN     NOT NULL,
    callee_ready               BOOLEAN     NOT NULL,
    caller_connected           BOOLEAN     NOT NULL,
    callee_connected           BOOLEAN     NOT NULL,
    caller_lease_expires_at    TIMESTAMPTZ NOT NULL,
    callee_lease_expires_at    TIMESTAMPTZ,
    caller_resume_request_id   UUID,
    callee_resume_request_id   UUID,
    offer_message_id           UUID,
    offer_digest               VARCHAR(64),
    answer_message_id          UUID,
    answer_digest              VARCHAR(64),
    caller_candidate_count     INTEGER     NOT NULL DEFAULT 0,
    callee_candidate_count     INTEGER     NOT NULL DEFAULT 0,
    server_instance_id         VARCHAR(64) NOT NULL,
    CONSTRAINT pk_call_runtime PRIMARY KEY (call_id),
    CONSTRAINT fk_call_runtime_call FOREIGN KEY (call_id) REFERENCES calls (id) ON DELETE CASCADE,
    CONSTRAINT chk_call_runtime_generation CHECK (generation >= 0),
    CONSTRAINT chk_call_runtime_signal_pairs CHECK (
        (offer_message_id IS NULL) = (offer_digest IS NULL)
        AND (answer_message_id IS NULL) = (answer_digest IS NULL)
        AND (answer_message_id IS NULL OR offer_message_id IS NOT NULL)
    ),
    CONSTRAINT chk_call_runtime_candidate_counts CHECK (
        caller_candidate_count BETWEEN 0 AND 256
        AND callee_candidate_count BETWEEN 0 AND 256
    )
);

CREATE INDEX idx_call_runtime_caller_lease ON call_runtime (caller_lease_expires_at);
CREATE INDEX idx_call_runtime_callee_lease ON call_runtime (callee_lease_expires_at)
    WHERE callee_lease_expires_at IS NOT NULL;

CREATE TABLE call_user_reservations
(
    user_id UUID NOT NULL,
    call_id UUID NOT NULL,
    CONSTRAINT pk_call_user_reservations PRIMARY KEY (user_id),
    CONSTRAINT fk_call_user_reservation_call FOREIGN KEY (call_id)
        REFERENCES calls (id) ON DELETE CASCADE
);

CREATE INDEX idx_call_user_reservations_call_id ON call_user_reservations (call_id);
