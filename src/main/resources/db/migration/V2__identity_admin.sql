-- Module: Identity + Admin

CREATE TABLE users
(
    id                  UUID PRIMARY KEY,
    email               VARCHAR(255) NOT NULL UNIQUE,
    password_hash       VARCHAR(100) NOT NULL,
    display_name        VARCHAR(60)  NOT NULL,
    avatar_key          VARCHAR(200),
    role                VARCHAR(10)  NOT NULL DEFAULT 'USER'
        CONSTRAINT users_role_chk CHECK (role IN ('USER', 'ADMIN')),
    status              VARCHAR(10)  NOT NULL DEFAULT 'ACTIVE'
        CONSTRAINT users_status_chk CHECK (status IN ('ACTIVE', 'BANNED')),
    home_area           geography(Point, 4326),
    verified_area_count SMALLINT     NOT NULL DEFAULT 0
        CONSTRAINT users_verified_area_count_chk CHECK (verified_area_count BETWEEN 0 AND 2),
    consent_at          TIMESTAMPTZ  NOT NULL,
    created_at          TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at          TIMESTAMPTZ  NOT NULL DEFAULT now(),
    version             BIGINT       NOT NULL DEFAULT 0,
    CONSTRAINT users_email_lowercase_chk CHECK (email = lower(email))
);

CREATE TABLE refresh_tokens
(
    id         UUID PRIMARY KEY,
    user_id    UUID        NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    token_hash VARCHAR(64) NOT NULL UNIQUE,
    family_id  UUID        NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL,
    revoked_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX refresh_tokens_user_id_idx ON refresh_tokens (user_id);
CREATE INDEX refresh_tokens_family_id_idx ON refresh_tokens (family_id);

CREATE TABLE user_blocks
(
    blocker_id UUID        NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    blocked_id UUID        NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (blocker_id, blocked_id),
    CONSTRAINT user_blocks_not_self_chk CHECK (blocker_id <> blocked_id)
);
CREATE INDEX user_blocks_blocked_id_idx ON user_blocks (blocked_id);

CREATE TABLE report_reasons
(
    id     SERIAL PRIMARY KEY,
    code   VARCHAR(30)  NOT NULL UNIQUE,
    label  VARCHAR(100) NOT NULL,
    active BOOLEAN      NOT NULL DEFAULT TRUE
);

CREATE TABLE reports
(
    id          UUID PRIMARY KEY,
    reporter_id UUID        NOT NULL, -- logical -> users
    target_type VARCHAR(10) NOT NULL
        CONSTRAINT reports_target_type_chk CHECK (target_type IN ('LISTING', 'USER', 'MESSAGE')),
    target_id   UUID        NOT NULL,
    reason_id   INT         NOT NULL REFERENCES report_reasons (id),
    description VARCHAR(500),
    status      VARCHAR(10) NOT NULL DEFAULT 'OPEN'
        CONSTRAINT reports_status_chk CHECK (status IN ('OPEN', 'RESOLVED', 'DISMISSED')),
    handled_by  UUID,                 -- logical -> users
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    handled_at  TIMESTAMPTZ
);
CREATE INDEX reports_status_created_at_idx ON reports (status, created_at);
CREATE INDEX reports_target_idx ON reports (target_type, target_id);

CREATE TABLE disputes
(
    id                  UUID PRIMARY KEY,
    transaction_id      UUID         NOT NULL, -- logical -> transactions
    review_id           UUID,                  -- logical -> reviews
    raised_by           UUID         NOT NULL, -- logical -> users
    against_user_id     UUID         NOT NULL, -- logical -> users
    reason              VARCHAR(500) NOT NULL,
    status              VARCHAR(20)  NOT NULL DEFAULT 'OPEN'
        CONSTRAINT disputes_status_chk CHECK (status IN ('OPEN', 'RESOLVED_UPHELD', 'RESOLVED_REJECTED')),
    handled_by          UUID,                  -- logical -> users
    resolution_note     VARCHAR(500),
    reputation_reversed BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at          TIMESTAMPTZ  NOT NULL DEFAULT now(),
    resolved_at         TIMESTAMPTZ,
    updated_at          TIMESTAMPTZ  NOT NULL DEFAULT now(),
    version             BIGINT       NOT NULL DEFAULT 0
);
CREATE INDEX disputes_transaction_id_idx ON disputes (transaction_id);
CREATE INDEX disputes_status_idx ON disputes (status);

-- Append-only, never updated
CREATE TABLE audit_log
(
    id          BIGSERIAL PRIMARY KEY,
    actor_id    UUID,                 -- logical -> users (NULL = system)
    action      VARCHAR(40) NOT NULL,
    target_type VARCHAR(20),
    target_id   UUID,
    detail      JSONB,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX audit_log_target_idx ON audit_log (target_type, target_id);
CREATE INDEX audit_log_created_at_idx ON audit_log (created_at);

CREATE TABLE app_parameters
(
    key         VARCHAR(60) PRIMARY KEY,
    value       VARCHAR(200) NOT NULL,
    description VARCHAR(200),
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT now()
);
