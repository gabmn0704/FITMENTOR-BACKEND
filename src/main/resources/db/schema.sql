CREATE TABLE IF NOT EXISTS users (
    id UUID PRIMARY KEY,
    name VARCHAR(120) NOT NULL,
    email VARCHAR(180) UNIQUE NOT NULL,
    level VARCHAR(30) NOT NULL DEFAULT 'BEGINNER',
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Google sign-in and profile fields. Guest users keep a synthetic email.
ALTER TABLE users ADD COLUMN IF NOT EXISTS google_id VARCHAR(64) UNIQUE;
ALTER TABLE users ADD COLUMN IF NOT EXISTS picture TEXT;
ALTER TABLE users ADD COLUMN IF NOT EXISTS goal VARCHAR(60);
ALTER TABLE users ADD COLUMN IF NOT EXISTS is_guest BOOLEAN NOT NULL DEFAULT false;
-- Databases where is_guest was created nullable: backfill and enforce NOT NULL (idempotent).
UPDATE users SET is_guest = false WHERE is_guest IS NULL;
ALTER TABLE users ALTER COLUMN is_guest SET NOT NULL;

-- Email/password sign-in. BCrypt hash; NULL for Google and guest users.
ALTER TABLE users ADD COLUMN IF NOT EXISTS password_hash VARCHAR(100);
-- Tokens issued before this instant are rejected. Set when Google links an email/password account.
ALTER TABLE users ADD COLUMN IF NOT EXISTS sessions_valid_after TIMESTAMP WITH TIME ZONE;

-- Emails are compared case-insensitively. The unique index is created only when no two accounts
-- differ just by case; otherwise it is skipped (NOTICE) so existing data keeps working.
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM users GROUP BY lower(email) HAVING COUNT(*) > 1) THEN
        RAISE NOTICE 'users has emails that differ only by case: unique index users_email_lower_key not created';
    ELSE
        CREATE UNIQUE INDEX IF NOT EXISTS users_email_lower_key ON users (lower(email));
    END IF;
END $$;

CREATE TABLE IF NOT EXISTS training_sessions (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    exercise VARCHAR(60) NOT NULL,
    repetitions INTEGER NOT NULL DEFAULT 0,
    accuracy NUMERIC(5,2) NOT NULL DEFAULT 0,
    calories NUMERIC(7,2) NOT NULL DEFAULT 0,
    duration_seconds INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

ALTER TABLE training_sessions ADD COLUMN IF NOT EXISTS avg_angles JSONB NOT NULL DEFAULT '{}'::jsonb;
UPDATE training_sessions SET avg_angles = '{}'::jsonb WHERE avg_angles IS NULL;
ALTER TABLE training_sessions ALTER COLUMN avg_angles SET NOT NULL;

-- Databases created before this change have the FK without ON DELETE CASCADE.
-- Recreate it only when the existing action is not CASCADE ('c').
DO $$
DECLARE
    fk_action TEXT;
BEGIN
    SELECT c.confdeltype::TEXT INTO fk_action
    FROM pg_constraint c
    WHERE c.conrelid = 'public.training_sessions'::regclass
      AND c.conname = 'training_sessions_user_id_fkey';

    IF fk_action IS DISTINCT FROM 'c' THEN
        ALTER TABLE training_sessions DROP CONSTRAINT IF EXISTS training_sessions_user_id_fkey;
        ALTER TABLE training_sessions
            ADD CONSTRAINT training_sessions_user_id_fkey
            FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE;
    END IF;
END $$;

CREATE TABLE IF NOT EXISTS user_achievements (
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    achievement_id VARCHAR(60) NOT NULL,
    unlocked_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (user_id, achievement_id)
);

CREATE TABLE IF NOT EXISTS routine_plans (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    level VARCHAR(30) NOT NULL,
    rest_seconds INTEGER NOT NULL DEFAULT 0,
    items JSONB NOT NULL DEFAULT '[]'::jsonb,
    reason TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS training_sessions_user_created_idx
    ON training_sessions (user_id, created_at DESC);

CREATE INDEX IF NOT EXISTS routine_plans_user_created_idx
    ON routine_plans (user_id, created_at DESC);

-- users.google_id is covered by the UNIQUE constraint declared above (unique index users_google_id_key).

-- Per-user aggregates. Users without sessions appear with zero totals and a NULL average accuracy.
CREATE OR REPLACE VIEW user_stats AS
SELECT
    u.id AS user_id,
    COUNT(ts.id) AS total_sessions,
    COALESCE(SUM(ts.repetitions), 0) AS total_repetitions,
    COALESCE(SUM(ts.calories), 0) AS total_calories,
    ROUND(AVG(ts.accuracy), 2) AS avg_accuracy
FROM users u
LEFT JOIN training_sessions ts ON ts.user_id = u.id
GROUP BY u.id;
