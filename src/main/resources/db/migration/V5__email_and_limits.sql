ALTER TABLE users ADD COLUMN email_verified_at timestamptz;

-- Single-use links for confirming an email or resetting a password. Only hashes are stored.
CREATE TABLE email_tokens (
    id          uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id     uuid NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    purpose     text NOT NULL CHECK (purpose IN ('VERIFY_EMAIL', 'RESET_PASSWORD')),
    token_hash  text NOT NULL UNIQUE,
    expires_at  timestamptz NOT NULL,
    used_at     timestamptz,
    created_at  timestamptz NOT NULL DEFAULT now()
);
CREATE INDEX idx_email_tokens_user ON email_tokens (user_id, purpose, created_at DESC);
CREATE INDEX idx_email_tokens_expiry ON email_tokens (expires_at);

-- Fixed-window counters, shared by every server instance. Rows hold IP addresses and are deleted within a day.
CREATE TABLE rate_limits (
    bucket        text NOT NULL,
    window_start  timestamptz NOT NULL,
    hits          integer NOT NULL DEFAULT 1,
    PRIMARY KEY (bucket, window_start)
);
CREATE INDEX idx_rate_limits_window ON rate_limits (window_start);

-- Total AI requests per day across all learners, as a hard ceiling on spending.
CREATE TABLE ai_global_usage (
    usage_date  date PRIMARY KEY,
    requests    integer NOT NULL DEFAULT 0
);
