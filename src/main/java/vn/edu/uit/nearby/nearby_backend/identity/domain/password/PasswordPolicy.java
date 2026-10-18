package vn.edu.uit.nearby.nearby_backend.identity.domain.password;

import java.util.List;
import java.util.Optional;

import vn.edu.uit.nearby.nearby_backend.identity.domain.EmailAddress;

public class PasswordPolicy {

	private final List<PasswordRule> rules;

	public PasswordPolicy(List<PasswordRule> rules) {
		this.rules = List.copyOf(rules);
	}

	/**
	 * @throws WeakPasswordException listing every violated rule, so the client can show
	 * all reasons at once
	 */
	public void validate(String password, EmailAddress owner) {
		PasswordCandidate candidate = new PasswordCandidate(password, owner);
		List<PasswordViolation> violations = rules.stream()
			.map(rule -> rule.check(candidate))
			.flatMap(Optional::stream)
			.toList();
		if (!violations.isEmpty()) {
			throw new WeakPasswordException(violations);
		}
	}

}
