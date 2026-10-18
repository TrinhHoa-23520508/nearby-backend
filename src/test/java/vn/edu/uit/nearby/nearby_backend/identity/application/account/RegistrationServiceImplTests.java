package vn.edu.uit.nearby.nearby_backend.identity.application.account;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static vn.edu.uit.nearby.nearby_backend.identity.domain.UserFixtures.NOW;

import java.time.Clock;
import java.time.Duration;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.AdditionalAnswers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import vn.edu.uit.nearby.nearby_backend.identity.application.IdentityParametersFixture;
import vn.edu.uit.nearby.nearby_backend.identity.application.port.OneTimeTokenPurpose;
import vn.edu.uit.nearby.nearby_backend.identity.application.port.OneTimeTokenStore;
import vn.edu.uit.nearby.nearby_backend.identity.domain.AccountStatus;
import vn.edu.uit.nearby.nearby_backend.identity.domain.User;
import vn.edu.uit.nearby.nearby_backend.identity.domain.UserFixtures;
import vn.edu.uit.nearby.nearby_backend.identity.domain.password.WeakPasswordException;
import vn.edu.uit.nearby.nearby_backend.identity.infrastructure.UserRepository;
import vn.edu.uit.nearby.nearby_backend.shared.error.BusinessException;
import vn.edu.uit.nearby.nearby_backend.shared.error.ConflictException;
import vn.edu.uit.nearby.nearby_backend.shared.error.InvalidRequestException;
import vn.edu.uit.nearby.nearby_backend.shared.error.RateLimitExceededException;
import vn.edu.uit.nearby.nearby_backend.shared.ratelimit.RateLimiter;

@ExtendWith(MockitoExtension.class)
class RegistrationServiceImplTests {

	private static final String CLIENT_IP = "203.0.113.7";

	private static final RegisterCommand VALID = new RegisterCommand("An.Nguyen@Example.com", "Secret123",
			"  An Nguyen ", CLIENT_IP);

	@Mock
	UserRepository users;

	@Mock
	PasswordEncoder passwordEncoder;

	@Mock
	OneTimeTokenStore tokens;

	@Mock
	AccountEmailDispatcher emails;

	@Mock
	RateLimiter rateLimiter;

	private RegistrationServiceImpl service() {
		return new RegistrationServiceImpl(users, PasswordPolicyFixture.standard(), passwordEncoder, tokens, emails,
				rateLimiter, IdentityParametersFixture.seeded(), Clock.fixed(NOW, ZoneOffset.UTC));
	}

	@Test
	void registrationCreatesAPendingAccountAndSendsTheVerificationEmail() {
		when(users.existsByEmail(UserFixtures.EMAIL)).thenReturn(false);
		when(passwordEncoder.encode("Secret123")).thenReturn("hash");
		when(users.save(any(User.class))).then(AdditionalAnswers.returnsFirstArg());

		RegisteredAccount account = service().register(VALID);

		assertThat(account.email()).isEqualTo(UserFixtures.EMAIL);
		assertThat(account.status()).isEqualTo(AccountStatus.PENDING);
		verify(rateLimiter).acquireOrThrow("registrations:" + CLIENT_IP, 10, Duration.ofHours(1));
		verify(emails).sendEmailVerification(
				argThat(user -> user.getId().equals(account.userId()) && user.getDisplayName().equals("An Nguyen")));
	}

	@Test
	void takenEmailIsRejectedWithoutSayingSo() {
		when(users.existsByEmail(UserFixtures.EMAIL)).thenReturn(true);

		assertThatThrownBy(() -> service().register(VALID)).isInstanceOf(ConflictException.class)
			.satisfies(ex -> assertThat(((BusinessException) ex).getCode()).isEqualTo("REGISTRATION_REJECTED"))
			.message()
			.doesNotContain(UserFixtures.EMAIL)
			.doesNotContainIgnoringCase("exist");
		verify(users, never()).save(any());
	}

	@Test
	void weakPasswordIsRejectedBeforeTheEmailIsLookedUp() {
		RegisterCommand weak = new RegisterCommand(UserFixtures.EMAIL, "short", "An", CLIENT_IP);

		assertThatThrownBy(() -> service().register(weak)).isInstanceOf(WeakPasswordException.class);
		verify(users, never()).existsByEmail(anyString());
	}

	@Test
	void tooManyRegistrationsFromOneIpAreRejected() {
		doThrow(new RateLimitExceededException()).when(rateLimiter).acquireOrThrow(anyString(), anyInt(), any());

		assertThatThrownBy(() -> service().register(VALID)).isInstanceOf(RateLimitExceededException.class);
		verify(users, never()).save(any());
	}

	@Test
	void validVerificationTokenActivatesTheAccount() {
		User user = UserFixtures.pendingUser();
		when(tokens.consume(OneTimeTokenPurpose.EMAIL_VERIFICATION, "token")).thenReturn(Optional.of(user.getId()));
		when(users.getRequired(user.getId())).thenReturn(user);

		service().verifyEmail("token");

		assertThat(user.getStatus()).isEqualTo(AccountStatus.ACTIVE);
		assertThat(user.getEmailVerifiedAt()).isEqualTo(NOW);
	}

	@Test
	void unknownOrUsedVerificationTokenIsRejected() {
		when(tokens.consume(OneTimeTokenPurpose.EMAIL_VERIFICATION, "token")).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service().verifyEmail("token")).isInstanceOf(InvalidRequestException.class)
			.extracting(ex -> ((BusinessException) ex).getCode())
			.isEqualTo("INVALID_VERIFICATION_TOKEN");
		verify(users, never()).getRequired(any(UUID.class));
	}

	@Test
	void resendSendsAnotherEmailToAnAccountAwaitingVerification() {
		User user = UserFixtures.pendingUser();
		when(users.findByEmail(UserFixtures.EMAIL)).thenReturn(Optional.of(user));

		service().resendVerification(UserFixtures.EMAIL);

		verify(emails).sendEmailVerification(user);
	}

	@Test
	void resendDoesNothingForVerifiedOrUnknownAccounts() {
		when(users.findByEmail(UserFixtures.EMAIL)).thenReturn(Optional.of(UserFixtures.activeUser()));
		when(users.findByEmail("nobody@example.com")).thenReturn(Optional.empty());

		service().resendVerification(UserFixtures.EMAIL);
		service().resendVerification("nobody@example.com");

		verify(emails, never()).sendEmailVerification(any());
	}

}
