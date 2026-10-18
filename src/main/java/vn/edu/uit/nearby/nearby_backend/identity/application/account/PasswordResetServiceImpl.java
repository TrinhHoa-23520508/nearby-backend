package vn.edu.uit.nearby.nearby_backend.identity.application.account;

import java.time.Clock;
import java.time.Instant;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import vn.edu.uit.nearby.nearby_backend.identity.application.auth.UserSessions;
import vn.edu.uit.nearby.nearby_backend.identity.application.port.OneTimeTokenPurpose;
import vn.edu.uit.nearby.nearby_backend.identity.application.port.OneTimeTokenStore;
import vn.edu.uit.nearby.nearby_backend.identity.domain.EmailAddress;
import vn.edu.uit.nearby.nearby_backend.identity.domain.User;
import vn.edu.uit.nearby.nearby_backend.identity.domain.password.PasswordPolicy;
import vn.edu.uit.nearby.nearby_backend.identity.infrastructure.UserRepository;
import vn.edu.uit.nearby.nearby_backend.shared.constant.ErrorCodes;
import vn.edu.uit.nearby.nearby_backend.shared.error.InvalidRequestException;

@Service
public class PasswordResetServiceImpl implements PasswordResetService {

	private final UserRepository users;

	private final PasswordPolicy passwordPolicy;

	private final PasswordEncoder passwordEncoder;

	private final OneTimeTokenStore tokens;

	private final AccountEmailDispatcher emails;

	private final UserSessions sessions;

	private final Clock clock;

	public PasswordResetServiceImpl(UserRepository users, PasswordPolicy passwordPolicy,
			PasswordEncoder passwordEncoder, OneTimeTokenStore tokens, AccountEmailDispatcher emails,
			UserSessions sessions, Clock clock) {
		this.users = users;
		this.passwordPolicy = passwordPolicy;
		this.passwordEncoder = passwordEncoder;
		this.tokens = tokens;
		this.emails = emails;
		this.sessions = sessions;
		this.clock = clock;
	}

	@Override
	@Transactional(readOnly = true)
	public void requestReset(String email) {
		users.findByEmail(EmailAddress.of(email).value())
			.filter(user -> !user.isLocked())
			.ifPresent(emails::sendPasswordReset);
	}

	@Override
	@Transactional
	public void resetPassword(String token, String newPassword) {
		User user = tokens.find(OneTimeTokenPurpose.PASSWORD_RESET, token)
			.flatMap(users::findById)
			.orElseThrow(PasswordResetServiceImpl::invalidToken);
		passwordPolicy.validate(newPassword, user.emailAddress());
		if (tokens.consume(OneTimeTokenPurpose.PASSWORD_RESET, token).isEmpty()) {
			throw invalidToken();
		}
		Instant now = clock.instant();
		user.changePassword(passwordEncoder.encode(newPassword), now);
		// Opening the reset link proves the user owns the address.
		user.verifyEmail(now);
		sessions.endAll(user.getId());
	}

	private static InvalidRequestException invalidToken() {
		return new InvalidRequestException(ErrorCodes.INVALID_PASSWORD_RESET_TOKEN,
				"The password reset link is invalid or has expired.");
	}

}
