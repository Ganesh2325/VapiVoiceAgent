-- =============================================================================
-- VoiceOS Database Schema — V1 Initial Migration
-- =============================================================================
-- All tables use UUID primary keys and UTC timestamps.
-- JSONB columns used for flexible metadata.
-- Never use Hibernate DDL — all schema changes via Flyway migrations.
-- =============================================================================

-- ─── Users ───────────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS users (
    id            UUID        NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    email         VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    display_name  VARCHAR(255) NOT NULL,
    role          VARCHAR(50)  NOT NULL DEFAULT 'USER',
    active        BOOLEAN      NOT NULL DEFAULT true,
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at    TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE UNIQUE INDEX IF NOT EXISTS idx_users_email ON users (email);

-- ─── Conversations ────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS conversations (
    id                UUID        NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    user_id           UUID        NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    title             VARCHAR(500),
    status            VARCHAR(50)  NOT NULL DEFAULT 'ACTIVE',
    voice_session_id  VARCHAR(255),
    active_agent_name VARCHAR(255),
    message_count     INTEGER      NOT NULL DEFAULT 0,
    created_at        TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at        TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_conversations_user_id ON conversations (user_id);
CREATE INDEX IF NOT EXISTS idx_conversations_user_status ON conversations (user_id, status);

-- ─── Messages ─────────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS messages (
    id              UUID        NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    conversation_id UUID        NOT NULL REFERENCES conversations(id) ON DELETE CASCADE,
    role            VARCHAR(50)  NOT NULL,  -- USER, ASSISTANT, SYSTEM, TOOL
    content         TEXT         NOT NULL,
    agent_name      VARCHAR(255),
    tool_name       VARCHAR(255),
    tool_call_id    VARCHAR(255),
    metadata        JSONB,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_messages_conversation_id ON messages (conversation_id);
CREATE INDEX IF NOT EXISTS idx_messages_conv_created ON messages (conversation_id, created_at);

-- ─── Agent Executions ─────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS agent_executions (
    id              UUID        NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    conversation_id UUID        REFERENCES conversations(id) ON DELETE SET NULL,
    user_id         UUID        NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    agent_name      VARCHAR(255) NOT NULL,
    input           TEXT,
    output          TEXT,
    status          VARCHAR(50)  NOT NULL DEFAULT 'PENDING',  -- PENDING, RUNNING, COMPLETED, FAILED, CANCELLED
    started_at      TIMESTAMPTZ,
    completed_at    TIMESTAMPTZ,
    duration_ms     BIGINT,
    error_message   TEXT,
    metadata        JSONB,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_agent_exec_conversation ON agent_executions (conversation_id);
CREATE INDEX IF NOT EXISTS idx_agent_exec_user ON agent_executions (user_id);
CREATE INDEX IF NOT EXISTS idx_agent_exec_status ON agent_executions (user_id, status);

-- ─── Tool Executions ──────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS tool_executions (
    id                 UUID        NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    agent_execution_id UUID        REFERENCES agent_executions(id) ON DELETE CASCADE,
    tool_name          VARCHAR(255) NOT NULL,
    input              JSONB,
    output             JSONB,
    status             VARCHAR(50)  NOT NULL DEFAULT 'PENDING',  -- PENDING, RUNNING, COMPLETED, FAILED, AWAITING_APPROVAL, REJECTED
    risk_level         VARCHAR(50)  NOT NULL DEFAULT 'LOW',      -- LOW, MEDIUM, HIGH, CRITICAL
    requires_approval  BOOLEAN      NOT NULL DEFAULT false,
    approval_id        UUID,
    started_at         TIMESTAMPTZ,
    completed_at       TIMESTAMPTZ,
    duration_ms        BIGINT,
    error_message      TEXT,
    created_at         TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_tool_exec_agent ON tool_executions (agent_execution_id);
CREATE INDEX IF NOT EXISTS idx_tool_exec_approval ON tool_executions (approval_id);
CREATE INDEX IF NOT EXISTS idx_tool_exec_status ON tool_executions (status);

-- ─── Agent Plans ──────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS agent_plans (
    id                 UUID        NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    conversation_id    UUID        REFERENCES conversations(id) ON DELETE SET NULL,
    user_id            UUID        NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    goal               TEXT         NOT NULL,
    steps              JSONB        NOT NULL DEFAULT '[]',
    status             VARCHAR(50)  NOT NULL DEFAULT 'PENDING',  -- PENDING, EXECUTING, COMPLETED, FAILED, CANCELLED
    current_step_index INTEGER      NOT NULL DEFAULT 0,
    created_at         TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at         TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_agent_plan_conversation ON agent_plans (conversation_id);
CREATE INDEX IF NOT EXISTS idx_agent_plan_user ON agent_plans (user_id);

-- ─── Tasks ────────────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS tasks (
    id              UUID        NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    user_id         UUID        NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    conversation_id UUID        REFERENCES conversations(id) ON DELETE SET NULL,
    title           VARCHAR(500) NOT NULL,
    description     TEXT,
    status          VARCHAR(50)  NOT NULL DEFAULT 'PENDING',  -- PENDING, IN_PROGRESS, COMPLETED, CANCELLED
    priority        VARCHAR(50)  NOT NULL DEFAULT 'MEDIUM',   -- LOW, MEDIUM, HIGH, URGENT
    due_date        TIMESTAMPTZ,
    completed_at    TIMESTAMPTZ,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_tasks_user_id ON tasks (user_id);
CREATE INDEX IF NOT EXISTS idx_tasks_user_status ON tasks (user_id, status);
CREATE INDEX IF NOT EXISTS idx_tasks_due_date ON tasks (due_date) WHERE due_date IS NOT NULL;

-- ─── Memory ───────────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS memory_entries (
    id          UUID        NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    user_id     UUID        NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    memory_type VARCHAR(50)  NOT NULL,   -- SHORT_TERM, LONG_TERM, EPISODIC, SEMANTIC
    key         VARCHAR(500),
    content     TEXT         NOT NULL,
    vector_id   VARCHAR(255),            -- Qdrant vector ID for semantic search
    source      VARCHAR(500),            -- conversation ID, agent name, document ID, etc.
    metadata    JSONB,
    expires_at  TIMESTAMPTZ,             -- NULL = permanent
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_memory_user_type ON memory_entries (user_id, memory_type);
CREATE INDEX IF NOT EXISTS idx_memory_key ON memory_entries (user_id, key);
CREATE INDEX IF NOT EXISTS idx_memory_expires ON memory_entries (expires_at) WHERE expires_at IS NOT NULL;

-- ─── Documents ────────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS documents (
    id            UUID        NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    user_id       UUID        NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    title         VARCHAR(500) NOT NULL,
    file_name     VARCHAR(500) NOT NULL,
    file_type     VARCHAR(100) NOT NULL,
    file_size     BIGINT,
    status        VARCHAR(50)  NOT NULL DEFAULT 'PROCESSING',  -- PROCESSING, INDEXED, FAILED
    chunk_count   INTEGER      NOT NULL DEFAULT 0,
    error_message TEXT,
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at    TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_documents_user_id ON documents (user_id);
CREATE INDEX IF NOT EXISTS idx_documents_status ON documents (user_id, status);

-- ─── Document Chunks ──────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS document_chunks (
    id          UUID        NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    document_id UUID        NOT NULL REFERENCES documents(id) ON DELETE CASCADE,
    content     TEXT         NOT NULL,
    chunk_index INTEGER      NOT NULL,
    vector_id   VARCHAR(255),          -- Qdrant vector ID
    metadata    JSONB,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_doc_chunk_document ON document_chunks (document_id);
CREATE INDEX IF NOT EXISTS idx_doc_chunk_vector ON document_chunks (vector_id) WHERE vector_id IS NOT NULL;

-- ─── Approvals ────────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS approvals (
    id                 UUID        NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    user_id            UUID        NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    conversation_id    UUID        REFERENCES conversations(id) ON DELETE SET NULL,
    agent_execution_id UUID        REFERENCES agent_executions(id) ON DELETE SET NULL,
    tool_execution_id  UUID,
    action_type        VARCHAR(255) NOT NULL,
    action_description TEXT         NOT NULL,
    risk_level         VARCHAR(50)  NOT NULL,    -- HIGH, CRITICAL
    payload            JSONB,
    status             VARCHAR(50)  NOT NULL DEFAULT 'PENDING',  -- PENDING, APPROVED, REJECTED, EXPIRED
    rejection_reason   TEXT,
    responded_at       TIMESTAMPTZ,
    expires_at         TIMESTAMPTZ,
    created_at         TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_approval_user_id ON approvals (user_id);
CREATE INDEX IF NOT EXISTS idx_approval_user_status ON approvals (user_id, status);
CREATE INDEX IF NOT EXISTS idx_approval_conversation ON approvals (conversation_id);
CREATE INDEX IF NOT EXISTS idx_approval_expires ON approvals (expires_at) WHERE expires_at IS NOT NULL;

-- ─── Webhook Events ───────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS webhook_events (
    id               UUID        NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    provider         VARCHAR(100) NOT NULL,
    event_type       VARCHAR(255) NOT NULL,
    payload          JSONB        NOT NULL,
    signature        VARCHAR(500),
    idempotency_key  VARCHAR(500) NOT NULL UNIQUE,
    processed        BOOLEAN      NOT NULL DEFAULT false,
    processed_at     TIMESTAMPTZ,
    error_message    TEXT,
    created_at       TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE UNIQUE INDEX IF NOT EXISTS idx_webhook_idempotency ON webhook_events (idempotency_key);
CREATE INDEX IF NOT EXISTS idx_webhook_provider_processed ON webhook_events (provider, processed);
CREATE INDEX IF NOT EXISTS idx_webhook_event_type ON webhook_events (provider, event_type);

-- ─── Evaluations ──────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS evaluations (
    id                 UUID           NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    conversation_id    UUID           REFERENCES conversations(id) ON DELETE SET NULL,
    agent_execution_id UUID           REFERENCES agent_executions(id) ON DELETE SET NULL,
    task_success       BOOLEAN,
    tool_success_rate  DOUBLE PRECISION,
    latency_ms         BIGINT,
    hallucination_score DOUBLE PRECISION,
    human_corrected    BOOLEAN        NOT NULL DEFAULT false,
    notes              TEXT,
    metadata           JSONB,
    created_at         TIMESTAMPTZ    NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_evaluation_conversation ON evaluations (conversation_id);
CREATE INDEX IF NOT EXISTS idx_evaluation_agent_exec ON evaluations (agent_execution_id);
CREATE INDEX IF NOT EXISTS idx_evaluation_created ON evaluations (created_at);

-- ─── Automatic updated_at Trigger ─────────────────────────────────────────────
-- Automatically updates the updated_at column on row changes
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ language 'plpgsql';

DO $$
DECLARE
    t TEXT;
BEGIN
    FOREACH t IN ARRAY ARRAY[
        'users', 'conversations', 'agent_plans', 'tasks', 'memory_entries', 'documents'
    ] LOOP
        EXECUTE format('
            DROP TRIGGER IF EXISTS trigger_update_%1$s_updated_at ON %1$s;
            CREATE TRIGGER trigger_update_%1$s_updated_at
                BEFORE UPDATE ON %1$s
                FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
        ', t);
    END LOOP;
END $$;
