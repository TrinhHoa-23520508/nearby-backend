package vn.edu.uit.nearby.nearby_backend.identity.domain.password;

import java.util.Locale;
import java.util.Optional;

public record EmailNotInPasswordRule(int minMatchLength) implements PasswordRule {

	public static final String RULE = "CONTAINS_EMAIL";

	@Override
	public Optional<PasswordViolation> check(PasswordCandidate candidate) {
		String localPart = candidate.owner().localPart();
		if (localPart.length() < minMatchLength || !candidate.password().toLowerCase(Locale.ROOT).contains(localPart)) {
			return Optional.empty();
		}
		return Optional.of(new PasswordViolation(RULE, "Password must not contain your email address."));
	}

}
