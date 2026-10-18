-- Module: Area Verification

-- At most area.max_count rows per user, enforced at the application layer (UC11)
CREATE TABLE user_areas
(
    id         UUID PRIMARY KEY,
    user_id    UUID                   NOT NULL, -- logical -> users
    center     geography(Point, 4326) NOT NULL,
    radius_m   INTEGER                NOT NULL
        CONSTRAINT user_areas_radius_chk CHECK (radius_m > 0),
    label      VARCHAR(100),
    created_at TIMESTAMPTZ            NOT NULL DEFAULT now()
);
CREATE INDEX user_areas_user_id_idx ON user_areas (user_id);

CREATE TABLE area_verifications
(
    id                UUID PRIMARY KEY,
    user_area_id      UUID        NOT NULL REFERENCES user_areas (id) ON DELETE CASCADE,
    -- Precise location; purged or rounded area.retention_days after verified_at (UC24).
    -- Declared NOT NULL in the data design; nullable here so the purge job can clear it.
    reported_location geography(Point, 4326),
    accuracy_m        REAL,
    distance_m        REAL,
    is_mock_suspected BOOLEAN     NOT NULL DEFAULT FALSE,
    verified_at       TIMESTAMPTZ NOT NULL,
    expires_at        TIMESTAMPTZ,
    created_at        TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX area_verifications_user_area_id_idx ON area_verifications (user_area_id);
CREATE INDEX area_verifications_expires_at_idx ON area_verifications (expires_at);
CREATE INDEX area_verifications_verified_at_idx ON area_verifications (verified_at);

-- Informational flags only, used for manual review (UC16); never auto-blocks verification
CREATE TABLE device_signals
(
    id                   UUID PRIMARY KEY,
    area_verification_id UUID        NOT NULL REFERENCES area_verifications (id) ON DELETE CASCADE,
    signal_code          VARCHAR(30) NOT NULL
        CONSTRAINT device_signals_code_chk CHECK (signal_code IN
                                                  ('MOCK_FLAG', 'MOCK_APP', 'EMULATOR_OR_ROOT', 'TIME_DRIFT',
                                                   'IMPOSSIBLE_SPEED', 'IP_MISMATCH', 'DEV_OPTIONS')),
    raw_value            VARCHAR(100),
    detected_at          TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX device_signals_area_verification_id_idx ON device_signals (area_verification_id);
