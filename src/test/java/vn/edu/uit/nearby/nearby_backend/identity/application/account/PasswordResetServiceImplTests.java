package vn.edu.uit.nearby.nearby_backend.identity.application.account;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static vn.edu.uit.nearby.nearby_backend.identity.domain.UserFixtures.NOW;

import java.time.Clock;
import java.time.ZoneOffset;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import vn.edu.uit.nearby.nearby_backend.identity.application.auth.UserSessions;
import vn.edu.uit.nearby.nearby_backend.identity.application.port.OneTimeTokenPurpose;
import vn.edu.uit.nearby.nearby_backend.identity.application.port.OneTimeTokenStore;
import vn.edu.uit.nearby.nearby_backend.identity.domain.AccountStatus;
import vn.edu.uit.nearby.nearby_backend.identity.domain.User;
import vn.edu.uit.nearby.nearby_backend.identity.domain.UserFixtures;
import vn.edu.uit.nearby.nearby_backend.identity.domain.password.WeakPasswordException;
import vn.edu.uit.nearby.nearby_backend.identity.infrastructure.UserRepository;
import vn.edu.uit.nearby.nearby_backend.shared.error.BusinessException;
import vn.edu.uit.nearby.nearby_backend.shared.error.InvalidRequestException;

@ExtendWith(MockitoExtension.class)
class PasswordResetServiceImplTests {

	private static final String TOKEN = "reset-token";

	private static final String NEW_PASSWORD = "NewSecret456";

	@Mock
	UserRepository users;

	@Mock
	PasswordEncoder passwordEncoder;

	@Mock
	OneTimeTokenStore tokens;

	@Mock
	AccountEmailDispatcher emails;

	@Mock
	UserSessions sessions;

	private PasswordResetServiceImpl service() {
		return new PasswordResetServiceImpl(users, PasswordPolicyFixture.standard(), passwordEncoder, tokens, emails,
				sessions, Clock.fixed(NOW, ZoneOffset.UTC));
	}

	@Test
	void resetLinkIsEmailedToAnAccountThatIsNotLocked() {
		User user = UserFixtures.activeUser();
		when(users.findByEmail(UserFixtures.EMAIL)).thenReturn(Optional.of(user));

		service().requestReset(" AN.NGUYEN@example.com");

		verify(emails).sendPasswordReset(user);
	}

	@Test
	void noEmailForLockedOrUnknownAccounts() {
		when(users.findByEmail(UserFixtures.EMAIL)).thenReturn(Optional.of(UserFixtures.lockedUser()));
		when(users.findByEmail("nobody@example.com")).thenReturn(Optional.empty());

		service().requestReset(UserFixtures.EMAIL);
		service().requestReset("nobody@example.com");

		verify(emails, never()).sendPasswordReset(any());
	}

	@Test
	void validTokenSetsTheNewPasswordVerifiesTheEmailAndEndsAllSessions() {
		User user = UserFixtures.pendingUser();
		givenTokenOf(user);
		when(tokens.consume(OneTimeTokenPurpose.PASSWORD_RESET, TOKEN)).thenReturn(Optional.of(user.getId()));
		when(passwordEncoder.encode(NEW_PASSWORD)).thenReturn("new-hash");

		service().resetPassword(TOKEN, NEW_PASSWORD);

		assertThat(user.getPasswordHash()).isEqualTo("new-hash");
		assertThat(user.getStatus()).isEqualTo(AccountStatus.ACTIVE);
		verify(sessions).endAll(user.getId());
	}

	@Test
	void unknownTokenIsRejected() {
		when(tokens.find(OneTimeTokenPurpose.PASSWORD_RESET, TOKEN)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service().resetPassword(TOKEN, NEW_PASSWORD))
			.isInstanceOf(InvalidRequestException.class)
			.extracting(ex -> ((BusinessException) ex).getCode())
			.isEqualTo("INVALID_PASSWORD_RESET_TOKEN");
	}

	@Test
	void weakNewPasswordKeepsTheTokenUsable() {
		givenTokenOf(UserFixtures.activeUser());

		assertThatThrownBy(() -> service().resetPassword(TOKEN, "weak")).isInstanceOf(WeakPasswordException.class);
		verify(tokens, never()).consume(any(), any());
	}

	@Test
	void tokenUsedByAConcurrentRequestIsRejected() {
		givenTokenOf(UserFixtures.activeUser());
		when(tokens.consume(OneTimeTokenPurpose.PASSWORD_RESET, TOKEN)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service().resetPassword(TOKEN, NEW_PASSWORD))
			.isInstanceOf(InvalidRequestException.class);
		verify(sessions, never()).endAll(any());
	}

	private void givenTokenOf(User user) {
		when(tokens.find(OneTimeTokenPurpose.PASSWORD_RESET, TOKEN)).thenReturn(Optional.of(user.getId()));
		when(users.findById(user.getId())).thenReturn(Optional.of(user));
	}

}
