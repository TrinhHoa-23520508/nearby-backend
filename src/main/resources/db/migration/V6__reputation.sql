-- Module: Reputation

CREATE TABLE reviews
(
    id             UUID PRIMARY KEY,
    transaction_id UUID        NOT NULL, -- logical -> transactions
    reviewer_id    UUID        NOT NULL, -- logical -> users
    reviewee_id    UUID        NOT NULL, -- logical -> users
    stars          SMALLINT    NOT NULL
        CONSTRAINT reviews_stars_chk CHECK (stars BETWEEN 1 AND 5),
    comment        VARCHAR(500),
    gps_confirmed  BOOLEAN     NOT NULL DEFAULT FALSE,
    status         VARCHAR(20) NOT NULL DEFAULT 'PUBLISHED'
        CONSTRAINT reviews_status_chk CHECK (status IN ('PUBLISHED', 'PENDING_MODERATION', 'HIDDEN')),
    created_at     TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT reviews_transaction_reviewer_uq UNIQUE (transaction_id, reviewer_id),
    CONSTRAINT reviews_not_self_chk CHECK (reviewer_id <> reviewee_id)
);
CREATE INDEX reviews_reviewee_created_at_idx ON reviews (reviewee_id, created_at);

-- The latest row per user is the current score
CREATE TABLE reputation_snapshots
(
    id           UUID PRIMARY KEY,
    user_id      UUID          NOT NULL, -- logical -> users
    score        NUMERIC(5, 2) NOT NULL
        CONSTRAINT reputation_snapshots_score_chk CHECK (score BETWEEN 0 AND 100),
    tier         VARCHAR(15)   NOT NULL
        CONSTRAINT reputation_snapshots_tier_chk CHECK (tier IN ('NEW', 'TRUSTED', 'HIGHLY_TRUSTED')),
    review_count INT           NOT NULL,
    penalty      NUMERIC(5, 2) NOT NULL DEFAULT 0,
    computed_at  TIMESTAMPTZ   NOT NULL DEFAULT now()
);
CREATE INDEX reputation_snapshots_user_computed_idx ON reputation_snapshots (user_id, computed_at DESC);
CREATE INDEX reputation_snapshots_computed_at_idx ON reputation_snapshots (computed_at);
