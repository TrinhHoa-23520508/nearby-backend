package vn.edu.uit.nearby.nearby_backend.identity.application.auth;

import java.time.Clock;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import vn.edu.uit.nearby.nearby_backend.identity.domain.EmailAddress;
import vn.edu.uit.nearby.nearby_backend.identity.domain.StoredRefreshToken;
import vn.edu.uit.nearby.nearby_backend.identity.domain.User;
import vn.edu.uit.nearby.nearby_backend.identity.infrastructure.RefreshTokenRepository;
import vn.edu.uit.nearby.nearby_backend.identity.infrastructure.UserRepository;
import vn.edu.uit.nearby.nearby_backend.shared.constant.ErrorCodes;
import vn.edu.uit.nearby.nearby_backend.shared.error.AuthenticationFailedException;
import vn.edu.uit.nearby.nearby_backend.shared.security.TokenService;

@Service
public class AuthServiceImpl implements AuthService {

	private final UserRepository users;

	private final RefreshTokenRepository refreshTokens;

	private final UserSessions sessions;

	private final LoginAttemptGuard loginAttempts;

	private final PasswordEncoder passwordEncoder;

	private final Clock clock;

	// Checked when the email is unknown, so an unknown email and a wrong password take
	// the same time and cannot be told apart.
	private final String unknownUserPasswordHash;

	public AuthServiceImpl(UserRepository users, RefreshTokenRepository refreshTokens, UserSessions sessions,
			LoginAttemptGuard loginAttempts, PasswordEncoder passwordEncoder, Clock clock) {
		this.users = users;
		this.refreshTokens = refreshTokens;
		this.sessions = sessions;
		this.loginAttempts = loginAttempts;
		this.passwordEncoder = passwordEncoder;
		this.clock = clock;
		this.unknownUserPasswordHash = passwordEncoder.encode(UUID.randomUUID().toString());
	}

	@Override
	@Transactional
	public TokenPair login(LoginCommand command) {
		EmailAddress email = EmailAddress.of(command.email());
		loginAttempts.ensureNotBlocked(email);
		User user = authenticate(email, command.password());
		loginAttempts.clearFailures(email);
		user.ensureCanLogIn();
		return sessions.start(user);
	}

	@Override
	@Transactional(noRollbackFor = AuthenticationFailedException.class)
	public TokenPair refresh(String refreshToken) {
		StoredRefreshToken stored = refreshTokens.findByTokenHash(TokenService.sha256Hex(refreshToken))
			.orElseThrow(AuthServiceImpl::invalidRefreshToken);
		Instant now = clock.instant();
		if (stored.isExpiredAt(now)) {
			throw invalidRefreshToken();
		}
		if (stored.isRevoked() || refreshTokens.revokeIfActive(stored.getId(), now) == 0) {
			refreshTokens.revokeFamily(stored.getFamilyId(), now);
			throw new AuthenticationFailedException(ErrorCodes.REFRESH_TOKEN_REUSED,
					"This session is no longer valid, please log in again.");
		}
		User user = users.getRequired(stored.getUserId());
		user.ensureCanLogIn();
		return sessions.rotate(user, stored.family());
	}

	@Override
	@Transactional
	public void logout(String refreshToken) {
		refreshTokens.findByTokenHash(TokenService.sha256Hex(refreshToken))
			.ifPresent(stored -> refreshTokens.revokeIfActive(stored.getId(), clock.instant()));
	}

	private User authenticate(EmailAddress email, String password) {
		Optional<User> user = users.findByEmail(email.value());
		String passwordHash = user.map(User::getPasswordHash).orElse(unknownUserPasswordHash);
		boolean isPasswordCorrect = passwordEncoder.matches(password, passwordHash);
		if (user.isEmpty() || !isPasswordCorrect) {
			loginAttempts.recordFailure(email);
			throw new AuthenticationFailedException(ErrorCodes.INVALID_CREDENTIALS, "Email or password is incorrect.");
		}
		return user.get();
	}

	private static AuthenticationFailedException invalidRefreshToken() {
		return new AuthenticationFailedException(ErrorCodes.INVALID_REFRESH_TOKEN,
				"The refresh token is invalid or expired, please log in again.");
	}

}
