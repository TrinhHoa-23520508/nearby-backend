package vn.edu.uit.nearby.nearby_backend.shared.error;

import java.io.Serial;

import vn.edu.uit.nearby.nearby_backend.shared.constant.ErrorCodes;

public non-sealed class RateLimitExceededException extends BusinessException {

	@Serial
	private static final long serialVersionUID = 1L;

	public RateLimitExceededException() {
		super(ErrorCodes.RATE_LIMITED, "Too many requests, please try again later.");
	}

}
