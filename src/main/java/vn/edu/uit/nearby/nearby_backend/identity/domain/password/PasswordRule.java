package vn.edu.uit.nearby.nearby_backend.identity.domain.password;

import java.util.Optional;

/**
 * One password strength check. {@link PasswordPolicy} applies every registered rule, so a
 * new rule is added by declaring it, without changing the policy.
 */
public interface PasswordRule {

	Optional<PasswordViolation> check(PasswordCandidate candidate);

}
