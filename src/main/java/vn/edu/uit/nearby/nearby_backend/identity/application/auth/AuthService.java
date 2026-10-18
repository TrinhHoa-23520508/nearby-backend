package vn.edu.uit.nearby.nearby_backend.identity.application.auth;

import vn.edu.uit.nearby.nearby_backend.shared.error.AuthenticationFailedException;

public interface AuthService {

	/**
	 * @throws AuthenticationFailedException {@code INVALID_CREDENTIALS} for an unknown
	 * email or a wrong password, without telling which
	 */
	TokenPair login(LoginCommand command);

	/**
	 * Rotates the refresh token: the presented token is revoked and a new pair is issued.
	 * @throws AuthenticationFailedException {@code REFRESH_TOKEN_REUSED} when an already
	 * rotated token is presented; its whole family is revoked
	 */
	TokenPair refresh(String refreshToken);

	/**
	 * Revokes the refresh token. Unknown, expired or revoked tokens are already logged
	 * out.
	 */
	void logout(String refreshToken);

}
