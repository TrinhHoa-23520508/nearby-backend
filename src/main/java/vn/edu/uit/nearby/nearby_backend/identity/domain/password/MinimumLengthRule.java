package vn.edu.uit.nearby.nearby_backend.identity.domain.password;

import java.util.Optional;

public record MinimumLengthRule(int minLength) implements PasswordRule {

	public static final String RULE = "MIN_LENGTH";

	@Override
	public Optional<PasswordViolation> check(PasswordCandidate candidate) {
		if (candidate.password().codePointCount(0, candidate.password().length()) >= minLength) {
			return Optional.empty();
		}
		return Optional.of(new PasswordViolation(RULE, "Password must be at least " + minLength + " characters."));
	}

}
