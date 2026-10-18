-- Module: Identity + Admin. Account lifecycle (FR-ID-01, FR-ID-05) and location consent (FR-ID-06).

-- New accounts stay PENDING until the email address is verified.
ALTER TABLE users DROP CONSTRAINT users_status_chk;
ALTER TABLE users
    ADD CONSTRAINT users_status_chk CHECK (status IN ('PENDING', 'ACTIVE', 'BANNED'));
ALTER TABLE users ALTER COLUMN status SET DEFAULT 'PENDING';
ALTER TABLE users ADD COLUMN email_verified_at TIMESTAMPTZ;

-- Consent is given before the first precise location collection, not at registration, and can be withdrawn.
ALTER TABLE users ALTER COLUMN consent_at DROP NOT NULL;
ALTER TABLE users ADD COLUMN consent_terms_version VARCHAR(20);
ALTER TABLE users ADD COLUMN consent_withdrawn_at TIMESTAMPTZ;
ALTER TABLE users
    ADD CONSTRAINT users_consent_terms_version_chk CHECK (consent_at IS NULL OR consent_terms_version IS NOT NULL);

CREATE INDEX users_created_at_id_idx ON users (created_at DESC, id DESC);
CREATE INDEX refresh_tokens_expires_at_idx ON refresh_tokens (expires_at);

INSERT INTO app_parameters (key, value, description)
VALUES ('auth.login_max_failed_attempts', '5', 'Failed logins per email before login is temporarily blocked'),
       ('auth.login_lockout_minutes', '15', 'Window (minutes) in which failed logins are counted'),
       ('auth.registrations_per_hour_per_ip', '10', 'Registrations accepted per client IP per hour'),
       ('auth.account_emails_per_hour', '5', 'Verification and password reset emails sent per address per hour'),
       ('auth.email_verification_ttl_hours', '24', 'Hours an email verification link stays valid'),
       ('auth.password_reset_ttl_minutes', '30', 'Minutes a password reset link stays valid'),
       ('consent.location_terms_version', '1.0', 'Current version of the location data processing terms'),
       ('image.max_bytes', '5242880', 'Maximum size (bytes) of an uploaded image');
