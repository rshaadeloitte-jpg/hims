-- PostgreSQL outbox queue semantics (Kafka retired): retry backoff gate + relay claim
-- stamp + due-row partial index. Same change as iam V5__pg_eventbus.sql; idempotent.
ALTER TABLE ihms_lab.outbox ADD COLUMN IF NOT EXISTS next_attempt_at TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE ihms_lab.outbox ADD COLUMN IF NOT EXISTS locked_at TIMESTAMP(3);
CREATE INDEX IF NOT EXISTS idx_outbox_due ON ihms_lab.outbox (next_attempt_at, created_at) WHERE status IN ('PENDING', 'FAILED');
