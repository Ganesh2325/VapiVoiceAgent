-- =============================================================================
-- VoiceOS — V4 Action steps and append-only audit events
-- =============================================================================
-- Ordered execution stages per action, plus immutable audit evidence.
-- Does not change V1–V3 tables.
-- =============================================================================

CREATE TABLE IF NOT EXISTS action_steps (
    id              UUID         NOT NULL PRIMARY KEY,
    action_id       UUID         NOT NULL REFERENCES actions(id) ON DELETE CASCADE,
    sequence_no     INTEGER      NOT NULL,
    step_type       VARCHAR(50)  NOT NULL,
    status          VARCHAR(50)  NOT NULL,
    started_at      TIMESTAMPTZ,
    completed_at    TIMESTAMPTZ,
    duration_ms     BIGINT,
    agent_name      VARCHAR(255),
    tool_name       VARCHAR(255),
    provider_name   VARCHAR(255),
    provider_mode   VARCHAR(50),
    request_id      VARCHAR(64),
    metadata_json   JSONB,
    result_json     JSONB,
    error_code      VARCHAR(100),
    error_message   TEXT,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_action_steps_order UNIQUE (action_id, sequence_no)
);

CREATE INDEX IF NOT EXISTS idx_action_steps_action ON action_steps (action_id);
CREATE INDEX IF NOT EXISTS idx_action_steps_action_status ON action_steps (action_id, status);
CREATE INDEX IF NOT EXISTS idx_action_steps_request ON action_steps (request_id);

CREATE TABLE IF NOT EXISTS audit_events (
    id              UUID         NOT NULL PRIMARY KEY,
    action_id       UUID         NOT NULL REFERENCES actions(id) ON DELETE CASCADE,
    action_step_id  UUID         REFERENCES action_steps(id) ON DELETE SET NULL,
    event_type      VARCHAR(80)  NOT NULL,
    event_key       VARCHAR(500) NOT NULL,
    occurred_at     TIMESTAMPTZ  NOT NULL,
    request_id      VARCHAR(64),
    call_id         VARCHAR(255),
    user_id         UUID,
    actor_type      VARCHAR(50)  NOT NULL,
    actor_id        VARCHAR(255),
    metadata_json   JSONB,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_audit_events_key UNIQUE (event_key)
);

CREATE INDEX IF NOT EXISTS idx_audit_events_action ON audit_events (action_id, occurred_at);
CREATE INDEX IF NOT EXISTS idx_audit_events_step ON audit_events (action_step_id);
CREATE INDEX IF NOT EXISTS idx_audit_events_request ON audit_events (request_id);
