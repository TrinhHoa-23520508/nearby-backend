package vn.edu.uit.nearby.nearby_backend.identity.application.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import vn.edu.uit.nearby.nearby_backend.identity.application.IdentityParametersFixture;
import vn.edu.uit.nearby.nearby_backend.identity.domain.EmailAddress;
import vn.edu.uit.nearby.nearby_backend.shared.error.RateLimitExceededException;
import vn.edu.uit.nearby.nearby_backend.shared.ratelimit.RateLimiter;

@ExtendWith(MockitoExtension.class)
class LoginAttemptGuardTests {

	private static final EmailAddress EMAIL = EmailAddress.of("an@example.com");

	private static final String KEY = "login-failures:an@example.com";

	@Mock
	RateLimiter rateLimiter;

	private LoginAttemptGuard guard() {
		return new LoginAttemptGuard(rateLimiter, IdentityParametersFixture.seeded());
	}

	@Test
	void emailUnderTheFailureLimitMayTryAgain() {
		when(rateLimiter.isExhausted(KEY, 5)).thenReturn(false);

		assertThatCode(() -> guard().ensureNotBlocked(EMAIL)).doesNotThrowAnyException();
	}

	@Test
	void emailAtTheFailureLimitIsBlockedUntilTheWindowEnds() {
		when(rateLimiter.isExhausted(KEY, 5)).thenReturn(true);
		when(rateLimiter.remainingWindow(KEY)).thenReturn(Duration.ofMinutes(7));

		assertThatThrownBy(() -> guard().ensureNotBlocked(EMAIL)).isInstanceOfSatisfying(
				RateLimitExceededException.class, ex -> assertThat(ex.getRetryAfter()).contains(Duration.ofMinutes(7)));
	}

	@Test
	void failureIsCountedInTheLockoutWindow() {
		guard().recordFailure(EMAIL);

		verify(rateLimiter).tryAcquire(KEY, 5, Duration.ofMinutes(15));
	}

	@Test
	void successfulLoginClearsTheFailures() {
		guard().clearFailures(EMAIL);

		verify(rateLimiter).reset(KEY);
	}

}
