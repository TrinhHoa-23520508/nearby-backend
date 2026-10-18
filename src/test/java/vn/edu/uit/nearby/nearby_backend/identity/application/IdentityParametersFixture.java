package vn.edu.uit.nearby.nearby_backend.identity.application;

import java.util.Map;

/**
 * The values seeded by V9__identity_account_lifecycle.sql.
 */
public final class IdentityParametersFixture {

	public static final Map<String, String> SEEDED = Map.of("auth.login_max_failed_attempts", "5",
			"auth.login_lockout_minutes", "15", "auth.registrations_per_hour_per_ip", "10",
			"auth.account_emails_per_hour", "5", "auth.email_verification_ttl_hours", "24",
			"auth.password_reset_ttl_minutes", "30", "consent.location_terms_version", "1.0", "image.max_bytes",
			"5242880");

	private IdentityParametersFixture() {
	}

	public static IdentityParameters seeded() {
		return new IdentityParameters(key -> {
			String value = SEEDED.get(key);
			if (value == null) {
				throw new IllegalStateException("Missing app parameter: " + key);
			}
			return value;
		});
	}

}
