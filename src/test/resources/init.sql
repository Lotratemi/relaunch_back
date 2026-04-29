CREATE TABLE IF NOT EXISTS "user" (
    id BIGSERIAL PRIMARY KEY,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    name TEXT NOT NULL,
    mail TEXT NOT NULL,
    age SMALLINT NOT NULL
);

CREATE TABLE IF NOT EXISTS objectives (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT REFERENCES "user"(id) ON DELETE CASCADE,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    end_at TIMESTAMPTZ NOT NULL,
    frequency BIGINT NOT NULL,
    title TEXT NOT NULL,
    description TEXT
);

CREATE TABLE IF NOT EXISTS user_profiles (
    id              SERIAL PRIMARY KEY,
    user_id         INT NOT NULL REFERENCES "user"(id) ON DELETE CASCADE,
    profile_type    TEXT NOT NULL,
    version         INT NOT NULL DEFAULT 1,
    raw_scores      JSONB,
    completed_at    TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS profiling_answers (
    id              SERIAL PRIMARY KEY,
    user_profile_id INT NOT NULL REFERENCES user_profiles(id) ON DELETE CASCADE,
    question_id     INT NOT NULL,
    question_text   TEXT NOT NULL,
    answer_value    TEXT NOT NULL,
    answered_at     TIMESTAMP NOT NULL DEFAULT NOW()
);

-- Psychological dimensions calculated (scores by axe)
CREATE TABLE IF NOT EXISTS profile_dimensions (
    id              SERIAL PRIMARY KEY,
    user_profile_id INT NOT NULL REFERENCES user_profiles(id) ON DELETE CASCADE,
    dimension       TEXT NOT NULL,   -- ex: "Leadership", "Empathie", "Analyse"
    score           FLOAT NOT NULL,  -- 0.0 à 1.0
    label           TEXT NOT NULL    -- "Élevé", "Modéré", "Faible"
);