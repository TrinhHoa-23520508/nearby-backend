package vn.edu.uit.nearby.nearby_backend.identity.application.account;

import vn.edu.uit.nearby.nearby_backend.identity.domain.password.WeakPasswordException;
import vn.edu.uit.nearby.nearby_backend.shared.error.InvalidRequestException;

public interface PasswordResetService {

	/**
	 * Emails a reset link when the email belongs to an account that is not locked;
	 * otherwise does nothing, so the response is the same for unknown emails (FR-ID-08).
	 */
	void requestReset(String email);

	/**
	 * Sets the new password and logs out every session of the account.
	 * @throws InvalidRequestException {@code INVALID_PASSWORD_RESET_TOKEN} for an
	 * unknown, used or expired token
	 * @throws WeakPasswordException when the new password breaks a rule; the token stays
	 * valid so the user can try another password
	 */
	void resetPassword(String token, String newPassword);

}
