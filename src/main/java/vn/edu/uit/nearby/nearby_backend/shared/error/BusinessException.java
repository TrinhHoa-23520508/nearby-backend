package vn.edu.uit.nearby.nearby_backend.shared.error;

import java.io.Serial;

import lombok.Getter;

@Getter
public abstract sealed class BusinessException extends RuntimeException permits ResourceNotFoundException,
		ConflictException, InvalidRequestException, OperationNotAllowedException, RateLimitExceededException {

	@Serial
	private static final long serialVersionUID = 1L;

	private final String code;

	protected BusinessException(String code, String message) {
		super(message);
		this.code = code;
	}

}
