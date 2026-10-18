package vn.edu.uit.nearby.nearby_backend.identity.domain.password;

import java.nio.charset.StandardCharsets;
import java.util.Optional;

/**
 * BCrypt only uses the first 72 bytes of a password and Spring Security rejects longer
 * ones, so the limit is in UTF-8 bytes, not characters.
 */
public record MaximumBytesRule(int maxBytes) implements PasswordRule {

	public static final String RULE = "MAX_LENGTH";

	@Override
	public Optional<PasswordViolation> check(PasswordCandidate candidate) {
		if (candidate.password().getBytes(StandardCharsets.UTF_8).length <= maxBytes) {
			return Optional.empty();
		}
		return Optional.of(new PasswordViolation(RULE, "Password must be at most " + maxBytes + " bytes long."));
	}

}
