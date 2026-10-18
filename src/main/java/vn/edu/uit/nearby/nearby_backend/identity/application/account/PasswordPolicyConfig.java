package vn.edu.uit.nearby.nearby_backend.identity.application.account;

import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import vn.edu.uit.nearby.nearby_backend.identity.domain.password.EmailNotInPasswordRule;
import vn.edu.uit.nearby.nearby_backend.identity.domain.password.LetterAndDigitRule;
import vn.edu.uit.nearby.nearby_backend.identity.domain.password.MaximumBytesRule;
import vn.edu.uit.nearby.nearby_backend.identity.domain.password.MinimumLengthRule;
import vn.edu.uit.nearby.nearby_backend.identity.domain.password.PasswordPolicy;
import vn.edu.uit.nearby.nearby_backend.identity.domain.password.PasswordRule;

/**
 * Every {@link PasswordRule} bean joins the policy; declaring a new bean adds a rule.
 */
@Configuration(proxyBeanMethods = false)
public class PasswordPolicyConfig {

	private static final int MIN_LENGTH = 8;

	private static final int BCRYPT_MAX_BYTES = 72;

	private static final int MIN_EMAIL_MATCH_LENGTH = 3;

	@Bean
	PasswordRule minimumLengthRule() {
		return new MinimumLengthRule(MIN_LENGTH);
	}

	@Bean
	PasswordRule maximumBytesRule() {
		return new MaximumBytesRule(BCRYPT_MAX_BYTES);
	}

	@Bean
	PasswordRule letterAndDigitRule() {
		return new LetterAndDigitRule();
	}

	@Bean
	PasswordRule emailNotInPasswordRule() {
		return new EmailNotInPasswordRule(MIN_EMAIL_MATCH_LENGTH);
	}

	@Bean
	PasswordPolicy passwordPolicy(List<PasswordRule> rules) {
		return new PasswordPolicy(rules);
	}

}
