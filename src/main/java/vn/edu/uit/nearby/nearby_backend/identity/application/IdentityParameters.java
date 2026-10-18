package vn.edu.uit.nearby.nearby_backend.identity.application;

import java.time.Duration;

import org.springframework.stereotype.Component;

import vn.edu.uit.nearby.nearby_backend.identity.AppParameters;

/**
 * Typed access to the identity module's runtime parameters; values are seeded by Flyway.
 */
@Component
public class IdentityParameters {

	private static final String LOGIN_MAX_FAILED_ATTEMPTS = "auth.login_max_failed_attempts";

	private static final String LOGIN_LOCKOUT_MINUTES = "auth.login_lockout_minutes";

	private static final String REGISTRATIONS_PER_HOUR_PER_IP = "auth.registrations_per_hour_per_ip";

	private static final String ACCOUNT_EMAILS_PER_HOUR = "auth.account_emails_per_hour";

	private static final String EMAIL_VERIFICATION_TTL_HOURS = "auth.email_verification_ttl_hours";

	private static final String PASSWORD_RESET_TTL_MINUTES = "auth.password_reset_ttl_minutes";

	private static final String LOCATION_TERMS_VERSION = "consent.location_terms_version";

	private static final String IMAGE_MAX_BYTES = "image.max_bytes";

	private final AppParameters parameters;

	public IdentityParameters(AppParameters parameters) {
		this.parameters = parameters;
	}

	public int loginMaxFailedAttempts() {
		return parameters.getInt(LOGIN_MAX_FAILED_ATTEMPTS);
	}

	public Duration loginLockoutWindow() {
		return Duration.ofMinutes(parameters.getInt(LOGIN_LOCKOUT_MINUTES));
	}

	public int registrationsPerHourPerIp() {
		return parameters.getInt(REGISTRATIONS_PER_HOUR_PER_IP);
	}

	public int accountEmailsPerHour() {
		return parameters.getInt(ACCOUNT_EMAILS_PER_HOUR);
	}

	public Duration emailVerificationTtl() {
		return Duration.ofHours(parameters.getInt(EMAIL_VERIFICATION_TTL_HOURS));
	}

	public Duration passwordResetTtl() {
		return Duration.ofMinutes(parameters.getInt(PASSWORD_RESET_TTL_MINUTES));
	}

	public String locationTermsVersion() {
		return parameters.get(LOCATION_TERMS_VERSION);
	}

	public long imageMaxBytes() {
		return parameters.getInt(IMAGE_MAX_BYTES);
	}

}
