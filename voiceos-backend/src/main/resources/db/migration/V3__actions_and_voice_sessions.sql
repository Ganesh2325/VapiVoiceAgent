-- =============================================================================
-- VoiceOS — V3 Canonical action persistence and voice sessions
-- =============================================================================
-- Actions are the durable record of the unified execution pipeline.
-- Voice sessions bind a Vapi callId to a VoiceOS user without trusting
-- customer.email as authentication.
-- =============================================================================

CREATE TABLE IF NOT EXISTS voice_sessions (
    id               UUID         NOT NULL PRIMARY KEY,
    call_id          VARCHAR(255) NOT NULL UNIQUE,
    user_id          UUID         REFERENCES users(id) ON DELETE SET NULL,
    conversation_id  UUID         REFERENCES conversations(id) ON DELETE SET NULL,
    status           VARCHAR(50)  NOT NULL DEFAULT 'UNBOUND',
    created_at       TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    bound_at         TIMESTAMPTZ,
    expires_at       TIMESTAMPTZ  NOT NULL,
    ended_at         TIMESTAMPTZ
);

CREATE INDEX IF NOT EXISTS idx_voice_sessions_user ON voice_sessions (user_id);
CREATE INDEX IF NOT EXISTS idx_voice_sessions_status ON voice_sessions (status);

CREATE TABLE IF NOT EXISTS actions (
    id                 UUID         NOT NULL PRIMARY KEY,
    user_id            UUID         REFERENCES users(id) ON DELETE SET NULL,
    conversation_id    UUID         REFERENCES conversations(id) ON DELETE SET NULL,
    call_id            VARCHAR(255),
    request_id         VARCHAR(64),
    event_id           VARCHAR(255),
    idempotency_key    VARCHAR(500) UNIQUE,
    action_type        VARCHAR(255),
    intent             TEXT,
    agent_name         VARCHAR(255),
    tool_name          VARCHAR(255),
    provider_name      VARCHAR(255),
    provider_mode      VARCHAR(50),
    status             VARCHAR(50)  NOT NULL,
    payload            JSONB,
    result             TEXT,
    error_message      TEXT,
    verification_passed BOOLEAN,
    created_at         TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    started_at         TIMESTAMPTZ,
    completed_at       TIMESTAMPTZ,
    duration_ms        BIGINT
);

CREATE INDEX IF NOT EXISTS idx_actions_user ON actions (user_id);
CREATE INDEX IF NOT EXISTS idx_actions_conversation ON actions (conversation_id);
CREATE INDEX IF NOT EXISTS idx_actions_call ON actions (call_id);
CREATE INDEX IF NOT EXISTS idx_actions_status ON actions (status);
