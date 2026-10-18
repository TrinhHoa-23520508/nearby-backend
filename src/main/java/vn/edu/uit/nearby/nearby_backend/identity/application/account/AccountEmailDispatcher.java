package vn.edu.uit.nearby.nearby_backend.identity.application.account;

import java.time.Duration;

import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import lombok.extern.slf4j.Slf4j;

import vn.edu.uit.nearby.nearby_backend.identity.application.IdentityParameters;
import vn.edu.uit.nearby.nearby_backend.identity.application.IdentityProperties;
import vn.edu.uit.nearby.nearby_backend.identity.application.port.AccountMailer;
import vn.edu.uit.nearby.nearby_backend.identity.application.port.OneTimeTokenPurpose;
import vn.edu.uit.nearby.nearby_backend.identity.application.port.OneTimeTokenStore;
import vn.edu.uit.nearby.nearby_backend.identity.domain.User;
import vn.edu.uit.nearby.nearby_backend.shared.constant.RedisKeyPrefixes;
import vn.edu.uit.nearby.nearby_backend.shared.ratelimit.RateLimiter;

/**
 * Issues a one-time token and emails its link. Over the hourly quota the email is skipped
 * silently: the endpoints that trigger it answer the same way whether or not the email
 * exists (FR-ID-08), so they must not start failing either.
 */
@Slf4j
@Component
public class AccountEmailDispatcher {

	private static final Duration QUOTA_WINDOW = Duration.ofHours(1);

	private static final String TOKEN_QUERY_PARAMETER = "token";

	private final OneTimeTokenStore tokens;

	private final AccountMailer mailer;

	private final RateLimiter rateLimiter;

	private final IdentityParameters parameters;

	private final IdentityProperties properties;

	public AccountEmailDispatcher(OneTimeTokenStore tokens, AccountMailer mailer, RateLimiter rateLimiter,
			IdentityParameters parameters, IdentityProperties properties) {
		this.tokens = tokens;
		this.mailer = mailer;
		this.rateLimiter = rateLimiter;
		this.parameters = parameters;
		this.properties = properties;
	}

	public void sendEmailVerification(User user) {
		if (isOverQuota(user)) {
			return;
		}
		String token = tokens.issue(OneTimeTokenPurpose.EMAIL_VERIFICATION, user.getId(),
				parameters.emailVerificationTtl());
		mailer.sendEmailVerification(user.emailAddress(), user.getDisplayName(),
				linkWithToken(properties.emailVerificationUrl(), token));
	}

	public void sendPasswordReset(User user) {
		if (isOverQuota(user)) {
			return;
		}
		String token = tokens.issue(OneTimeTokenPurpose.PASSWORD_RESET, user.getId(), parameters.passwordResetTtl());
		mailer.sendPasswordReset(user.emailAddress(), user.getDisplayName(),
				linkWithToken(properties.passwordResetUrl(), token));
	}

	private boolean isOverQuota(User user) {
		boolean isWithinQuota = rateLimiter.tryAcquire(RedisKeyPrefixes.ACCOUNT_EMAILS + user.getEmail(),
				parameters.accountEmailsPerHour(), QUOTA_WINDOW);
		if (!isWithinQuota) {
			log.info("Account email to user {} skipped: hourly quota reached", user.getId());
		}
		return !isWithinQuota;
	}

	private static String linkWithToken(String baseUrl, String token) {
		return UriComponentsBuilder.fromUriString(baseUrl).queryParam(TOKEN_QUERY_PARAMETER, token).toUriString();
	}

}
