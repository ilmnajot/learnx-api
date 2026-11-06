ALTER TABLE users ADD COLUMN role text NOT NULL DEFAULT 'USER' CHECK (role IN ('USER', 'ADMIN'));
ALTER TABLE users ADD COLUMN banned_at timestamptz;
CREATE INDEX idx_users_created ON users (created_at DESC);

ALTER TABLE videos ADD COLUMN hidden_at timestamptz;

-- One public review per learner. Editing a review sends it back for moderation.
CREATE TABLE comments (
    id            uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id       uuid NOT NULL UNIQUE REFERENCES users (id) ON DELETE CASCADE,
    body          text NOT NULL CHECK (char_length(body) BETWEEN 10 AND 500),
    rating        integer NOT NULL CHECK (rating BETWEEN 1 AND 5),
    status        text NOT NULL DEFAULT 'PENDING' CHECK (status IN ('PENDING', 'APPROVED', 'REJECTED')),
    created_at    timestamptz NOT NULL DEFAULT now(),
    updated_at    timestamptz NOT NULL DEFAULT now(),
    moderated_at  timestamptz
);
CREATE INDEX idx_comments_status ON comments (status, updated_at DESC);

-- Settings the admin can change without redeploying. Stored here rather than in code or git.
CREATE TABLE site_settings (
    key         text PRIMARY KEY,
    value       text NOT NULL,
    updated_at  timestamptz NOT NULL DEFAULT now()
);

-- Who did what. Keeps the email as text, so entries survive the admin's account being deleted.
CREATE TABLE admin_audit (
    id           uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    admin_id     uuid NOT NULL,
    admin_email  text NOT NULL,
    action       text NOT NULL,
    target       text,
    created_at   timestamptz NOT NULL DEFAULT now()
);
CREATE INDEX idx_admin_audit_created ON admin_audit (created_at DESC);
