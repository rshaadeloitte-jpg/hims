-- PostgreSQL-native event bus (replaces Kafka).
--
-- Shared platform schema ihms_eventbus on the shared `hims` database. It lives
-- in iam's Flyway because iam is the only bundle that runs Flyway on startup.
--
--   event_log     one row per published envelope (topic = former Kafka topic name,
--                 DLQ topics keep their dlt.* names). UNIQUE (topic, event_id) makes
--                 a relay retry a no-op.
--   subscription  consumer group -> topic. A group only sees events published after
--                 it subscribed (Kafka auto.offset.reset=latest semantics).
--   delivery      per-group work queue, filled by trigger on event_log insert and
--                 claimed with FOR UPDATE SKIP LOCKED. PENDING -> PROCESSING ->
--                 DONE, or back to PENDING with backoff, or FAILED (dead letter).

CREATE SCHEMA IF NOT EXISTS ihms_eventbus;

CREATE TABLE IF NOT EXISTS ihms_eventbus.event_log (
    id             BIGSERIAL    PRIMARY KEY,
    topic          VARCHAR(200) NOT NULL,
    event_id       UUID         NOT NULL,
    event_type     VARCHAR(100),
    partition_key  VARCHAR(100),
    envelope       JSONB        NOT NULL,
    created_at     TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_event_log_topic_event UNIQUE (topic, event_id)
);
CREATE INDEX IF NOT EXISTS idx_event_log_created_at ON ihms_eventbus.event_log (created_at);

CREATE TABLE IF NOT EXISTS ihms_eventbus.subscription (
    consumer_group VARCHAR(100) NOT NULL,
    topic          VARCHAR(200) NOT NULL,
    created_at     TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    CONSTRAINT pk_subscription PRIMARY KEY (consumer_group, topic)
);

CREATE TABLE IF NOT EXISTS ihms_eventbus.delivery (
    id              BIGSERIAL    PRIMARY KEY,
    consumer_group  VARCHAR(100) NOT NULL,
    event_log_id    BIGINT       NOT NULL REFERENCES ihms_eventbus.event_log (id) ON DELETE CASCADE,
    status          VARCHAR(20)  NOT NULL DEFAULT 'PENDING',
    retry_count     INTEGER      NOT NULL DEFAULT 0,
    next_attempt_at TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    locked_at       TIMESTAMPTZ,
    delivered_at    TIMESTAMPTZ,
    last_error      TEXT,
    CONSTRAINT uq_delivery_group_event UNIQUE (consumer_group, event_log_id),
    CONSTRAINT ck_delivery_status CHECK (status IN ('PENDING', 'PROCESSING', 'DONE', 'FAILED'))
);
CREATE INDEX IF NOT EXISTS idx_delivery_due ON ihms_eventbus.delivery (consumer_group, next_attempt_at, id)
    WHERE status = 'PENDING';
CREATE INDEX IF NOT EXISTS idx_delivery_processing ON ihms_eventbus.delivery (consumer_group, locked_at)
    WHERE status = 'PROCESSING';

-- Fan-out: every subscribed group gets its own delivery row, then NOTIFY wakes
-- listeners (payload = topic). Polling remains the delivery guarantee.
CREATE OR REPLACE FUNCTION ihms_eventbus.fan_out() RETURNS trigger
LANGUAGE plpgsql AS $$
BEGIN
    INSERT INTO ihms_eventbus.delivery (consumer_group, event_log_id)
    SELECT s.consumer_group, NEW.id
    FROM ihms_eventbus.subscription s
    WHERE s.topic = NEW.topic
    ON CONFLICT (consumer_group, event_log_id) DO NOTHING;
    PERFORM pg_notify('ihms_eventbus', NEW.topic);
    RETURN NEW;
END;
$$;

DROP TRIGGER IF EXISTS trg_event_log_fan_out ON ihms_eventbus.event_log;
CREATE TRIGGER trg_event_log_fan_out
    AFTER INSERT ON ihms_eventbus.event_log
    FOR EACH ROW EXECUTE FUNCTION ihms_eventbus.fan_out();

-- Retention: drop log rows older than the window once no delivery is still
-- outstanding (delivery rows cascade). FAILED deliveries pin their event.
CREATE OR REPLACE PROCEDURE ihms_eventbus.purge(p_retention INTERVAL DEFAULT INTERVAL '7 days')
LANGUAGE plpgsql AS $$
BEGIN
    DELETE FROM ihms_eventbus.event_log e
    WHERE e.created_at < NOW() - p_retention
      AND NOT EXISTS (SELECT 1 FROM ihms_eventbus.delivery d
                      WHERE d.event_log_id = e.id AND d.status <> 'DONE');
END;
$$;

DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM pg_extension WHERE extname = 'pg_cron') THEN
        PERFORM cron.schedule('ihms-eventbus-purge', '17 * * * *', 'CALL ihms_eventbus.purge()');
    END IF;
END;
$$;

-- Outbox queue semantics on every module outbox (all share one column layout).
-- next_attempt_at drives retry backoff; locked_at records the relay's claim.
-- Guarded so a fresh database where a module has not been created yet still
-- migrates; the per-module copies of this ALTER cover that case later.
DO $$
DECLARE
    t TEXT;
BEGIN
    FOREACH t IN ARRAY ARRAY[
        'ihms_adt', 'ihms_nursing', 'ihms_ot', 'ihms_appointment', 'ihms_billing',
        'ihms_payment', 'ihms_rcm', 'ihms_ordering', 'ihms_lab', 'ihms_patient',
        'ihms_item', 'ihms_master'
    ] LOOP
        IF to_regclass(t || '.outbox') IS NOT NULL THEN
            EXECUTE format('ALTER TABLE %I.outbox ADD COLUMN IF NOT EXISTS next_attempt_at TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP', t);
            EXECUTE format('ALTER TABLE %I.outbox ADD COLUMN IF NOT EXISTS locked_at TIMESTAMP(3)', t);
            EXECUTE format('CREATE INDEX IF NOT EXISTS idx_outbox_due ON %I.outbox (next_attempt_at, created_at) WHERE status IN (''PENDING'', ''FAILED'')', t);
        END IF;
    END LOOP;
END;
$$;
