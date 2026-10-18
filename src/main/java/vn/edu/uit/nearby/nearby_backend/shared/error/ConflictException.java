package vn.edu.uit.nearby.nearby_backend.shared.error;

import java.io.Serial;

public non-sealed class ConflictException extends BusinessException {

	@Serial
	private static final long serialVersionUID = 1L;

	public ConflictException(String code, String message) {
		super(code, message);
	}

}
