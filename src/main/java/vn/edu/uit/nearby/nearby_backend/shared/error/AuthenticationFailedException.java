package vn.edu.uit.nearby.nearby_backend.shared.error;

import java.io.Serial;

public non-sealed class AuthenticationFailedException extends BusinessException {

	@Serial
	private static final long serialVersionUID = 1L;

	public AuthenticationFailedException(String code, String message) {
		super(code, message);
	}

}
