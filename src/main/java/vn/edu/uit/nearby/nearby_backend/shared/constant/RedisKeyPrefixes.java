package vn.edu.uit.nearby.nearby_backend.shared.constant;

public final class RedisKeyPrefixes {

	public static final String RATE_LIMIT = "rl:";

	public static final String IDEMPOTENCY = "idem:";

	public static final String EMAIL_VERIFICATION_TOKEN = "ev:";

	public static final String PASSWORD_RESET_TOKEN = "pr:";

	public static final String LOCKED_ACCOUNT = "locked:";

	public static final String LOGIN_FAILURES = "login-failures:";

	public static final String REGISTRATIONS_BY_IP = "registrations:";

	public static final String ACCOUNT_EMAILS = "account-emails:";

	private RedisKeyPrefixes() {
	}

}
