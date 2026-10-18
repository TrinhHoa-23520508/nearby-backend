package vn.edu.uit.nearby.nearby_backend.identity.domain.password;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import vn.edu.uit.nearby.nearby_backend.identity.domain.EmailAddress;

class PasswordPolicyTests {

	private static final EmailAddress OWNER = EmailAddress.of("binh.tran@example.com");

	private final PasswordPolicy policy = new PasswordPolicy(List.of(new MinimumLengthRule(8), new MaximumBytesRule(72),
			new LetterAndDigitRule(), new EmailNotInPasswordRule(3)));

	@Test
	void strongPasswordPasses() {
		assertThatCode(() -> policy.validate("Correct7Horse", OWNER)).doesNotThrowAnyException();
	}

	@Test
	void passwordOfExactlyEightCharactersPasses() {
		assertThatCode(() -> policy.validate("abcdef12", OWNER)).doesNotThrowAnyException();
	}

	@Test
	void sevenCharactersAreTooShort() {
		assertThat(violatedRules("abcde12")).containsExactly(MinimumLengthRule.RULE);
	}

	@Test
	void lengthCountsCharactersNotBytes() {
		assertThat(new MinimumLengthRule(8).check(candidate("mậtkhẩu1"))).isEmpty();
	}

	@Test
	void seventyTwoBytesPassButSeventyThreeDoNot() {
		String seventyTwoBytes = "a1" + "b".repeat(70);

		assertThat(violatedRules(seventyTwoBytes)).isEmpty();
		assertThat(violatedRules(seventyTwoBytes + "c")).containsExactly(MaximumBytesRule.RULE);
	}

	@ParameterizedTest
	@ValueSource(strings = { "onlyletters", "1234567890" })
	void passwordNeedsLettersAndDigits(String password) {
		assertThat(violatedRules(password)).containsExactly(LetterAndDigitRule.RULE);
	}

	@Test
	void passwordContainingTheEmailLocalPartIsRejectedCaseInsensitively() {
		assertThat(violatedRules("BINH.TRAN2026")).containsExactly(EmailNotInPasswordRule.RULE);
	}

	@Test
	void veryShortEmailLocalPartIsNotMatched() {
		assertThat(new EmailNotInPasswordRule(3).check(new PasswordCandidate("ab123456x", EmailAddress.of("ab@x.vn"))))
			.isEmpty();
	}

	@Test
	void everyViolatedRuleIsReportedAtOnce() {
		assertThatThrownBy(() -> policy.validate("abc", OWNER)).isInstanceOfSatisfying(WeakPasswordException.class,
				ex -> {
					assertThat(ex.getViolations()).extracting(PasswordViolation::rule)
						.containsExactly(MinimumLengthRule.RULE, LetterAndDigitRule.RULE);
					assertThat(ex.getCode()).isEqualTo("WEAK_PASSWORD");
					assertThat(ex.getMessage()).contains("at least 8 characters", "letters and digits");
					assertThat(ex.getProperties()).containsEntry("violations", ex.getViolations());
				});
	}

	@Test
	void nullPasswordIsTreatedAsEmpty() {
		assertThat(violatedRules(null)).contains(MinimumLengthRule.RULE);
	}

	@Test
	void addingARuleNeedsNoChangeToThePolicy() {
		PasswordRule noSpaces = candidate -> candidate.password().contains(" ")
				? Optional.of(new PasswordViolation("NO_SPACES", "No spaces.")) : Optional.empty();
		PasswordPolicy extended = new PasswordPolicy(List.of(new MinimumLengthRule(8), noSpaces));

		assertThatThrownBy(() -> extended.validate("with space1", OWNER))
			.isInstanceOfSatisfying(WeakPasswordException.class, ex -> assertThat(ex.getProperties())
				.isEqualTo(Map.of("violations", List.of(new PasswordViolation("NO_SPACES", "No spaces.")))));
	}

	private List<String> violatedRules(String password) {
		try {
			policy.validate(password, OWNER);
			return List.of();
		}
		catch (WeakPasswordException ex) {
			return ex.getViolations().stream().map(PasswordViolation::rule).toList();
		}
	}

	private static PasswordCandidate candidate(String password) {
		return new PasswordCandidate(password, OWNER);
	}

}
