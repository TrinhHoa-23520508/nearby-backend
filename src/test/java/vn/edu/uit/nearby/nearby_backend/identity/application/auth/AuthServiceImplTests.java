package vn.edu.uit.nearby.nearby_backend.identity.application.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static vn.edu.uit.nearby.nearby_backend.identity.domain.UserFixtures.NOW;

import java.time.Clock;
import java.time.Duration;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import vn.edu.uit.nearby.nearby_backend.identity.domain.EmailAddress;
import vn.edu.uit.nearby.nearby_backend.identity.domain.StoredRefreshToken;
import vn.edu.uit.nearby.nearby_backend.identity.domain.StoredRefreshToken.RefreshTokenFamily;
import vn.edu.uit.nearby.nearby_backend.identity.domain.User;
import vn.edu.uit.nearby.nearby_backend.identity.domain.UserFixtures;
import vn.edu.uit.nearby.nearby_backend.identity.infrastructure.RefreshTokenRepository;
import vn.edu.uit.nearby.nearby_backend.identity.infrastructure.UserRepository;
import vn.edu.uit.nearby.nearby_backend.shared.error.AuthenticationFailedException;
import vn.edu.uit.nearby.nearby_backend.shared.error.BusinessException;
import vn.edu.uit.nearby.nearby_backend.shared.error.OperationNotAllowedException;
import vn.edu.uit.nearby.nearby_backend.shared.error.RateLimitExceededException;
import vn.edu.uit.nearby.nearby_backend.shared.security.TokenService;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTests {

	private static final String PASSWORD = "Secret123";

	private static final String DUMMY_HASH = "{bcrypt}dummy";

	private static final String RAW_REFRESH_TOKEN = "raw-refresh-token";

	private static final TokenPair TOKENS = new TokenPair("access", NOW.plus(Duration.ofMinutes(15)), "refresh",
			NOW.plus(Duration.ofDays(30)));

	@Mock
	UserRepository users;

	@Mock
	RefreshTokenRepository refreshTokens;

	@Mock
	UserSessions sessions;

	@Mock
	LoginAttemptGuard loginAttempts;

	@Mock
	PasswordEncoder passwordEncoder;

	private AuthServiceImpl authService;

	@BeforeEach
	void createService() {
		when(passwordEncoder.encode(anyString())).thenReturn(DUMMY_HASH);
		authService = new AuthServiceImpl(users, refreshTokens, sessions, loginAttempts, passwordEncoder,
				Clock.fixed(NOW, ZoneOffset.UTC));
	}

	@Test
	void correctPasswordStartsASessionAndClearsFailures() {
		User user = UserFixtures.activeUser();
		when(users.findByEmail(UserFixtures.EMAIL)).thenReturn(Optional.of(user));
		when(passwordEncoder.matches(PASSWORD, UserFixtures.PASSWORD_HASH)).thenReturn(true);
		when(sessions.start(user)).thenReturn(TOKENS);

		TokenPair tokens = authService.login(new LoginCommand(" An.Nguyen@Example.com", PASSWORD));

		assertThat(tokens).isEqualTo(TOKENS);
		verify(loginAttempts).clearFailures(EmailAddress.of(UserFixtures.EMAIL));
	}

	@Test
	void wrongPasswordIsRecordedAsAFailedAttempt() {
		when(users.findByEmail(UserFixtures.EMAIL)).thenReturn(Optional.of(UserFixtures.activeUser()));
		when(passwordEncoder.matches(PASSWORD, UserFixtures.PASSWORD_HASH)).thenReturn(false);

		assertThatThrownBy(() -> authService.login(new LoginCommand(UserFixtures.EMAIL, PASSWORD)))
			.isInstanceOf(AuthenticationFailedException.class)
			.extracting(AuthServiceImplTests::codeOf)
			.isEqualTo("INVALID_CREDENTIALS");
		verify(loginAttempts).recordFailure(EmailAddress.of(UserFixtures.EMAIL));
		verifyNoInteractions(sessions);
	}

	@Test
	void unknownEmailStillChecksAPasswordAndFailsTheSameWay() {
		when(users.findByEmail(UserFixtures.EMAIL)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> authService.login(new LoginCommand(UserFixtures.EMAIL, PASSWORD)))
			.isInstanceOf(AuthenticationFailedException.class)
			.hasMessage("Email or password is incorrect.");
		verify(passwordEncoder).matches(PASSWORD, DUMMY_HASH);
		verify(loginAttempts).recordFailure(EmailAddress.of(UserFixtures.EMAIL));
	}

	@Test
	void blockedEmailIsRejectedBeforeThePasswordIsChecked() {
		doThrow(new RateLimitExceededException(Duration.ofMinutes(10))).when(loginAttempts)
			.ensureNotBlocked(EmailAddress.of(UserFixtures.EMAIL));

		assertThatThrownBy(() -> authService.login(new LoginCommand(UserFixtures.EMAIL, PASSWORD)))
			.isInstanceOf(RateLimitExceededException.class);
		verify(passwordEncoder, never()).matches(anyString(), anyString());
	}

	@Test
	void unverifiedAccountCannotLogInEvenWithTheRightPassword() {
		when(users.findByEmail(UserFixtures.EMAIL)).thenReturn(Optional.of(UserFixtures.pendingUser()));
		when(passwordEncoder.matches(PASSWORD, UserFixtures.PASSWORD_HASH)).thenReturn(true);

		assertThatThrownBy(() -> authService.login(new LoginCommand(UserFixtures.EMAIL, PASSWORD)))
			.isInstanceOf(OperationNotAllowedException.class)
			.extracting(AuthServiceImplTests::codeOf)
			.isEqualTo("EMAIL_NOT_VERIFIED");
		verifyNoInteractions(sessions);
	}

	@Test
	void validRefreshTokenIsRevokedAndRotatedWithinItsFamily() {
		User user = UserFixtures.activeUser();
		StoredRefreshToken stored = storedToken(user);
		when(refreshTokens.revokeIfActive(stored.getId(), NOW)).thenReturn(1);
		when(users.getRequired(stored.getUserId())).thenReturn(user);
		when(sessions.rotate(user, stored.family())).thenReturn(TOKENS);

		assertThat(authService.refresh(RAW_REFRESH_TOKEN)).isEqualTo(TOKENS);
		verify(refreshTokens, never()).revokeFamily(any(), any());
	}

	@Test
	void unknownRefreshTokenIsRejected() {
		when(refreshTokens.findByTokenHash(anyString())).thenReturn(Optional.empty());

		assertThatThrownBy(() -> authService.refresh(RAW_REFRESH_TOKEN))
			.isInstanceOf(AuthenticationFailedException.class)
			.extracting(AuthServiceImplTests::codeOf)
			.isEqualTo("INVALID_REFRESH_TOKEN");
	}

	@Test
	void expiredRefreshTokenRequiresANewLoginWithoutRevokingTheFamily() {
		StoredRefreshToken stored = StoredRefreshToken.issue(RefreshTokenFamily.startFor(UUID.randomUUID()),
				TokenService.sha256Hex(RAW_REFRESH_TOKEN), NOW, NOW.minus(Duration.ofDays(30)));
		when(refreshTokens.findByTokenHash(TokenService.sha256Hex(RAW_REFRESH_TOKEN))).thenReturn(Optional.of(stored));

		assertThatThrownBy(() -> authService.refresh(RAW_REFRESH_TOKEN)).extracting(AuthServiceImplTests::codeOf)
			.isEqualTo("INVALID_REFRESH_TOKEN");
		verify(refreshTokens, never()).revokeFamily(any(), any());
	}

	@Test
	void reusingARotatedTokenRevokesTheWholeFamily() {
		StoredRefreshToken stored = storedToken(UserFixtures.activeUser());
		ReflectionTestUtils.setField(stored, "revokedAt", NOW.minus(Duration.ofMinutes(1)));

		assertThatThrownBy(() -> authService.refresh(RAW_REFRESH_TOKEN))
			.isInstanceOf(AuthenticationFailedException.class)
			.extracting(AuthServiceImplTests::codeOf)
			.isEqualTo("REFRESH_TOKEN_REUSED");
		verify(refreshTokens).revokeFamily(stored.getFamilyId(), NOW);
		verifyNoInteractions(sessions);
	}

	@Test
	void losingAConcurrentRefreshIsTreatedAsReuse() {
		StoredRefreshToken stored = storedToken(UserFixtures.activeUser());
		when(refreshTokens.revokeIfActive(stored.getId(), NOW)).thenReturn(0);

		assertThatThrownBy(() -> authService.refresh(RAW_REFRESH_TOKEN)).extracting(AuthServiceImplTests::codeOf)
			.isEqualTo("REFRESH_TOKEN_REUSED");
		verify(refreshTokens).revokeFamily(stored.getFamilyId(), NOW);
	}

	@Test
	void lockedAccountCannotRefresh() {
		User user = UserFixtures.lockedUser();
		StoredRefreshToken stored = storedToken(user);
		when(refreshTokens.revokeIfActive(stored.getId(), NOW)).thenReturn(1);
		when(users.getRequired(stored.getUserId())).thenReturn(user);

		assertThatThrownBy(() -> authService.refresh(RAW_REFRESH_TOKEN)).extracting(AuthServiceImplTests::codeOf)
			.isEqualTo("ACCOUNT_LOCKED");
	}

	@Test
	void logoutRevokesTheRefreshToken() {
		StoredRefreshToken stored = storedToken(UserFixtures.activeUser());

		authService.logout(RAW_REFRESH_TOKEN);

		verify(refreshTokens).revokeIfActive(stored.getId(), NOW);
	}

	@Test
	void logoutWithAnUnknownTokenIsAlreadyLoggedOut() {
		when(refreshTokens.findByTokenHash(anyString())).thenReturn(Optional.empty());

		authService.logout(RAW_REFRESH_TOKEN);

		verify(refreshTokens, never()).revokeIfActive(any(), any());
	}

	private StoredRefreshToken storedToken(User user) {
		StoredRefreshToken stored = StoredRefreshToken.issue(RefreshTokenFamily.startFor(user.getId()),
				TokenService.sha256Hex(RAW_REFRESH_TOKEN), NOW.plus(Duration.ofDays(29)),
				NOW.minus(Duration.ofDays(1)));
		when(refreshTokens.findByTokenHash(TokenService.sha256Hex(RAW_REFRESH_TOKEN))).thenReturn(Optional.of(stored));
		return stored;
	}

	private static String codeOf(Throwable ex) {
		return ((BusinessException) ex).getCode();
	}

}
