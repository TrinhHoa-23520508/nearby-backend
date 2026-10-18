package vn.edu.uit.nearby.nearby_backend.identity.application.account;

import vn.edu.uit.nearby.nearby_backend.identity.domain.password.WeakPasswordException;
import vn.edu.uit.nearby.nearby_backend.shared.error.ConflictException;

public interface RegistrationService {

	/**
	 * Creates a PENDING account and emails a verification link.
	 * @throws WeakPasswordException listing every violated password rule
	 * @throws ConflictException {@code REGISTRATION_REJECTED} when the email is taken;
	 * the message does not say so
	 */
	RegisteredAccount register(RegisterCommand command);

	/**
	 * Activates the account of a valid, unused verification token.
	 */
	void verifyEmail(String token);

	/**
	 * Emails a new link when the account still awaits verification; otherwise does
	 * nothing, so the caller cannot learn whether the email is registered.
	 */
	void resendVerification(String email);

}
