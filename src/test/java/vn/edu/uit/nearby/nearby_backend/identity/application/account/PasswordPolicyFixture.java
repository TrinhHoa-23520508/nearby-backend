package vn.edu.uit.nearby.nearby_backend.identity.application.account;

import java.util.List;

import vn.edu.uit.nearby.nearby_backend.identity.domain.password.PasswordPolicy;

/**
 * The policy exactly as {@link PasswordPolicyConfig} wires it in the application.
 */
public final class PasswordPolicyFixture {

	private static final PasswordPolicyConfig CONFIG = new PasswordPolicyConfig();

	private PasswordPolicyFixture() {
	}

	public static PasswordPolicy standard() {
		return CONFIG.passwordPolicy(List.of(CONFIG.minimumLengthRule(), CONFIG.maximumBytesRule(),
				CONFIG.letterAndDigitRule(), CONFIG.emailNotInPasswordRule()));
	}

}
