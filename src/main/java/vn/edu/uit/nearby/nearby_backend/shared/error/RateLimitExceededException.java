package vn.edu.uit.nearby.nearby_backend.shared.error;

import java.io.Serial;
import java.time.Duration;
import java.util.Optional;

import org.jspecify.annotations.Nullable;

import vn.edu.uit.nearby.nearby_backend.shared.constant.ErrorCodes;

public non-sealed class RateLimitExceededException extends BusinessException {

	@Serial
	private static final long serialVersionUID = 1L;

	private final @Nullable Duration retryAfter;

	public RateLimitExceededException() {
		this(null);
	}

	public RateLimitExceededException(@Nullable Duration retryAfter) {
		super(ErrorCodes.RATE_LIMITED, "Too many requests, please try again later.");
		this.retryAfter = retryAfter;
	}

	public Optional<Duration> getRetryAfter() {
		return Optional.ofNullable(retryAfter);
	}

}
