package vn.edu.uit.nearby.nearby_backend.identity.domain.password;

import java.util.Optional;

public record LetterAndDigitRule() implements PasswordRule {

	public static final String RULE = "LETTER_AND_DIGIT";

	@Override
	public Optional<PasswordViolation> check(PasswordCandidate candidate) {
		String password = candidate.password();
		boolean hasLetter = password.codePoints().anyMatch(Character::isLetter);
		boolean hasDigit = password.codePoints().anyMatch(Character::isDigit);
		if (hasLetter && hasDigit) {
			return Optional.empty();
		}
		return Optional.of(new PasswordViolation(RULE, "Password must contain both letters and digits."));
	}

}
