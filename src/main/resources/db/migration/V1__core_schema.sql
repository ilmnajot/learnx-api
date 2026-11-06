-- Identity
CREATE TABLE users (
    id                    uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    email                 text NOT NULL UNIQUE CHECK (email = lower(email)),
    password_hash         text NOT NULL,
    display_name          text NOT NULL,
    job_role              text,
    goal                  text,
    level                 text CHECK (level IN ('A1', 'A2', 'B1', 'B2', 'C1')),
    native_language       text NOT NULL DEFAULT 'uz',
    timezone              text NOT NULL DEFAULT 'Asia/Tashkent',
    reminder_time         time,
    onboarded_at          timestamptz,
    failed_login_attempts integer NOT NULL DEFAULT 0,
    locked_until          timestamptz,
    created_at            timestamptz NOT NULL DEFAULT now(),
    updated_at            timestamptz NOT NULL DEFAULT now()
);

CREATE TABLE refresh_tokens (
    id          uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id     uuid NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    token_hash  text NOT NULL UNIQUE,
    expires_at  timestamptz NOT NULL,
    revoked_at  timestamptz,
    created_at  timestamptz NOT NULL DEFAULT now()
);
CREATE INDEX idx_refresh_tokens_user ON refresh_tokens (user_id);
CREATE INDEX idx_refresh_tokens_expiry ON refresh_tokens (expires_at);

-- Content
CREATE TABLE topics (
    id        uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    slug      text NOT NULL UNIQUE,
    title     text NOT NULL,
    position  integer NOT NULL UNIQUE
);

CREATE TABLE lessons (
    id            uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    topic_id      uuid NOT NULL REFERENCES topics (id),
    slug          text NOT NULL UNIQUE,
    title         text NOT NULL,
    can_do        text NOT NULL,
    scenario      text NOT NULL,
    ai_persona    text NOT NULL,
    opening_line  text NOT NULL,
    position      integer NOT NULL UNIQUE,
    est_minutes   integer NOT NULL DEFAULT 5 CHECK (est_minutes > 0)
);
CREATE INDEX idx_lessons_topic ON lessons (topic_id);

CREATE TABLE phrases (
    id             uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    lesson_id      uuid NOT NULL REFERENCES lessons (id) ON DELETE CASCADE,
    english        text NOT NULL,
    translation    text NOT NULL,
    example        text NOT NULL,
    position       integer NOT NULL,
    UNIQUE (lesson_id, position)
);

CREATE TABLE placement_questions (
    id             uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    prompt         text NOT NULL,
    options        text[] NOT NULL,
    correct_index  integer NOT NULL,
    weight         integer NOT NULL DEFAULT 1,
    position       integer NOT NULL UNIQUE
);

-- Learner progress
CREATE TABLE lesson_progress (
    user_id       uuid NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    lesson_id     uuid NOT NULL REFERENCES lessons (id),
    completed_at  timestamptz NOT NULL DEFAULT now(),
    PRIMARY KEY (user_id, lesson_id)
);

CREATE TABLE phrase_reviews (
    user_id        uuid NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    phrase_id      uuid NOT NULL REFERENCES phrases (id),
    ease           numeric(4, 2) NOT NULL DEFAULT 2.50,
    interval_days  integer NOT NULL DEFAULT 0,
    repetitions    integer NOT NULL DEFAULT 0,
    lapses         integer NOT NULL DEFAULT 0,
    due_at         timestamptz NOT NULL,
    reviewed_at    timestamptz,
    PRIMARY KEY (user_id, phrase_id)
);
CREATE INDEX idx_phrase_reviews_due ON phrase_reviews (user_id, due_at);

CREATE TABLE activity_days (
    user_id        uuid NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    activity_date  date NOT NULL,
    events         integer NOT NULL DEFAULT 1,
    points         integer NOT NULL DEFAULT 0,
    PRIMARY KEY (user_id, activity_date)
);
CREATE INDEX idx_activity_days_date ON activity_days (activity_date, user_id);

-- Roleplay
CREATE TABLE roleplay_sessions (
    id            uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id       uuid NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    lesson_id     uuid NOT NULL REFERENCES lessons (id),
    max_turns     integer NOT NULL DEFAULT 3,
    turn_count    integer NOT NULL DEFAULT 0,
    created_at    timestamptz NOT NULL DEFAULT now(),
    completed_at  timestamptz
);
CREATE INDEX idx_roleplay_sessions_user ON roleplay_sessions (user_id, created_at DESC);

CREATE TABLE roleplay_turns (
    id             uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    session_id     uuid NOT NULL REFERENCES roleplay_sessions (id) ON DELETE CASCADE,
    turn_number    integer NOT NULL,
    prompt_line    text NOT NULL,
    user_text      text NOT NULL,
    improved_text  text NOT NULL,
    tips           text[] NOT NULL,
    reply_line     text NOT NULL,
    provider       text NOT NULL,
    created_at     timestamptz NOT NULL DEFAULT now(),
    UNIQUE (session_id, turn_number)
);

CREATE TABLE ai_usage (
    user_id     uuid NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    usage_date  date NOT NULL,
    requests    integer NOT NULL DEFAULT 0,
    PRIMARY KEY (user_id, usage_date)
);

CREATE TABLE game_results (
    id          uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id     uuid NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    game        text NOT NULL,
    correct     integer NOT NULL CHECK (correct >= 0),
    total       integer NOT NULL CHECK (total > 0 AND correct <= total),
    points      integer NOT NULL DEFAULT 0,
    played_on   date NOT NULL,
    created_at  timestamptz NOT NULL DEFAULT now()
);
CREATE INDEX idx_game_results_user_day ON game_results (user_id, played_on);

-- Squads
CREATE TABLE squads (
    id           uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    name         text NOT NULL,
    invite_code  text NOT NULL UNIQUE,
    owner_id     uuid NOT NULL REFERENCES users (id),
    created_at   timestamptz NOT NULL DEFAULT now()
);

CREATE TABLE squad_members (
    squad_id   uuid NOT NULL REFERENCES squads (id) ON DELETE CASCADE,
    user_id    uuid NOT NULL UNIQUE REFERENCES users (id) ON DELETE CASCADE,
    joined_at  timestamptz NOT NULL DEFAULT now(),
    PRIMARY KEY (squad_id, user_id)
);

CREATE TABLE squad_incidents (
    id           uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    squad_id     uuid NOT NULL REFERENCES squads (id) ON DELETE CASCADE,
    week_start   date NOT NULL,
    title        text NOT NULL,
    points       integer NOT NULL DEFAULT 0,
    created_at   timestamptz NOT NULL DEFAULT now(),
    UNIQUE (squad_id, week_start)
);

CREATE TABLE squad_contributions (
    incident_id  uuid NOT NULL REFERENCES squad_incidents (id) ON DELETE CASCADE,
    user_id      uuid NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    points       integer NOT NULL DEFAULT 0,
    PRIMARY KEY (incident_id, user_id)
);
