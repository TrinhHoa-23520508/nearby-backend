package vn.edu.uit.nearby.nearby_backend.identity.application.account;

import java.time.Clock;
import java.time.Duration;
import java.util.UUID;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import vn.edu.uit.nearby.nearby_backend.identity.application.IdentityParameters;
import vn.edu.uit.nearby.nearby_backend.identity.application.port.OneTimeTokenPurpose;
import vn.edu.uit.nearby.nearby_backend.identity.application.port.OneTimeTokenStore;
import vn.edu.uit.nearby.nearby_backend.identity.domain.EmailAddress;
import vn.edu.uit.nearby.nearby_backend.identity.domain.User;
import vn.edu.uit.nearby.nearby_backend.identity.domain.password.PasswordPolicy;
import vn.edu.uit.nearby.nearby_backend.identity.infrastructure.UserRepository;
import vn.edu.uit.nearby.nearby_backend.shared.constant.ErrorCodes;
import vn.edu.uit.nearby.nearby_backend.shared.constant.RedisKeyPrefixes;
import vn.edu.uit.nearby.nearby_backend.shared.error.ConflictException;
import vn.edu.uit.nearby.nearby_backend.shared.error.InvalidRequestException;
import vn.edu.uit.nearby.nearby_backend.shared.ratelimit.RateLimiter;

@Service
public class RegistrationServiceImpl implements RegistrationService {

	private static final Duration REGISTRATION_WINDOW = Duration.ofHours(1);

	private final UserRepository users;

	private final PasswordPolicy passwordPolicy;

	private final PasswordEncoder passwordEncoder;

	private final OneTimeTokenStore tokens;

	private final AccountEmailDispatcher emails;

	private final RateLimiter rateLimiter;

	private final IdentityParameters parameters;

	private final Clock clock;

	public RegistrationServiceImpl(UserRepository users, PasswordPolicy passwordPolicy, PasswordEncoder passwordEncoder,
			OneTimeTokenStore tokens, AccountEmailDispatcher emails, RateLimiter rateLimiter,
			IdentityParameters parameters, Clock clock) {
		this.users = users;
		this.passwordPolicy = passwordPolicy;
		this.passwordEncoder = passwordEncoder;
		this.tokens = tokens;
		this.emails = emails;
		this.rateLimiter = rateLimiter;
		this.parameters = parameters;
		this.clock = clock;
	}

	@Override
	@Transactional
	public RegisteredAccount register(RegisterCommand command) {
		rateLimiter.acquireOrThrow(RedisKeyPrefixes.REGISTRATIONS_BY_IP + command.clientIp(),
				parameters.registrationsPerHourPerIp(), REGISTRATION_WINDOW);
		EmailAddress email = EmailAddress.of(command.email());
		passwordPolicy.validate(command.password(), email);
		if (users.existsByEmail(email.value())) {
			throw new ConflictException(ErrorCodes.REGISTRATION_REJECTED,
					"Registration could not be completed with the provided details.");
		}
		User user = users.save(User.register(email, passwordEncoder.encode(command.password()),
				command.displayName().trim(), clock.instant()));
		emails.sendEmailVerification(user);
		return RegisteredAccount.of(user);
	}

	@Override
	@Transactional
	public void verifyEmail(String token) {
		UUID userId = tokens.consume(OneTimeTokenPurpose.EMAIL_VERIFICATION, token)
			.orElseThrow(() -> new InvalidRequestException(ErrorCodes.INVALID_VERIFICATION_TOKEN,
					"The verification link is invalid or has expired."));
		users.getRequired(userId).verifyEmail(clock.instant());
	}

	@Override
	@Transactional(readOnly = true)
	public void resendVerification(String email) {
		users.findByEmail(EmailAddress.of(email).value())
			.filter(User::isAwaitingEmailVerification)
			.ifPresent(emails::sendEmailVerification);
	}

}
