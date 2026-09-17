-- =============================================================================
-- VoiceOS — V2 Webhook idempotency result storage
-- =============================================================================
-- Adds columns required to return a previously recorded Vapi webhook result
-- when the same logical event is delivered more than once.
-- Unique constraint on idempotency_key already exists from V1.
-- =============================================================================

ALTER TABLE webhook_events ADD COLUMN IF NOT EXISTS response_json TEXT;
ALTER TABLE webhook_events ADD COLUMN IF NOT EXISTS call_id VARCHAR(255);
ALTER TABLE webhook_events ADD COLUMN IF NOT EXISTS event_id VARCHAR(255);
ALTER TABLE webhook_events ADD COLUMN IF NOT EXISTS request_id VARCHAR(64);

CREATE INDEX IF NOT EXISTS idx_webhook_call_id ON webhook_events (call_id);
CREATE INDEX IF NOT EXISTS idx_webhook_event_id ON webhook_events (event_id);
