package vn.edu.uit.nearby.nearby_backend.identity.domain;

import java.time.Instant;

public final class UserFixtures {

	public static final Instant NOW = Instant.parse("2026-10-18T03:00:00Z");

	public static final String EMAIL = "an.nguyen@example.com";

	public static final String PASSWORD_HASH = "{noop}Secret123";

	public static final String DISPLAY_NAME = "An Nguyen";

	private UserFixtures() {
	}

	public static User pendingUser() {
		return pendingUser(EMAIL);
	}

	public static User pendingUser(String email) {
		return User.register(EmailAddress.of(email), PASSWORD_HASH, DISPLAY_NAME, NOW);
	}

	public static User activeUser() {
		return activeUser(EMAIL);
	}

	public static User activeUser(String email) {
		User user = pendingUser(email);
		user.verifyEmail(NOW);
		return user;
	}

	public static User lockedUser() {
		User user = activeUser();
		user.lock(NOW);
		return user;
	}

	public static User admin() {
		return User.administrator(EmailAddress.of("admin@example.com"), PASSWORD_HASH, "Admin", NOW);
	}

}
