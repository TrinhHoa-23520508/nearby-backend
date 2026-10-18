package vn.edu.uit.nearby.nearby_backend.identity.application.auth;

import org.springframework.stereotype.Component;

import vn.edu.uit.nearby.nearby_backend.identity.application.IdentityParameters;
import vn.edu.uit.nearby.nearby_backend.identity.domain.EmailAddress;
import vn.edu.uit.nearby.nearby_backend.shared.constant.RedisKeyPrefixes;
import vn.edu.uit.nearby.nearby_backend.shared.error.RateLimitExceededException;
import vn.edu.uit.nearby.nearby_backend.shared.ratelimit.RateLimiter;

/**
 * Blocks login for an email after too many failed attempts within the lockout window
 * (UC02: more than 5 failures in 15 minutes).
 */
@Component
public class LoginAttemptGuard {

	private final RateLimiter rateLimiter;

	private final IdentityParameters parameters;

	public LoginAttemptGuard(RateLimiter rateLimiter, IdentityParameters parameters) {
		this.rateLimiter = rateLimiter;
		this.parameters = parameters;
	}

	public void ensureNotBlocked(EmailAddress email) {
		String key = keyOf(email);
		if (rateLimiter.isExhausted(key, parameters.loginMaxFailedAttempts())) {
			throw new RateLimitExceededException(rateLimiter.remainingWindow(key));
		}
	}

	public void recordFailure(EmailAddress email) {
		rateLimiter.tryAcquire(keyOf(email), parameters.loginMaxFailedAttempts(), parameters.loginLockoutWindow());
	}

	public void clearFailures(EmailAddress email) {
		rateLimiter.reset(keyOf(email));
	}

	private static String keyOf(EmailAddress email) {
		return RedisKeyPrefixes.LOGIN_FAILURES + email.value();
	}

}
