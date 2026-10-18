package vn.edu.uit.nearby.nearby_backend.shared.error;

import java.io.Serial;
import java.util.Map;

import lombok.Getter;

@Getter
public abstract sealed class BusinessException extends RuntimeException
		permits ResourceNotFoundException, ConflictException, InvalidRequestException, OperationNotAllowedException,
		RateLimitExceededException, AuthenticationFailedException {

	@Serial
	private static final long serialVersionUID = 1L;

	private final String code;

	protected BusinessException(String code, String message) {
		super(message);
		this.code = code;
	}

	/**
	 * Extra members added to the problem detail, for example the list of violated rules.
	 */
	public Map<String, Object> getProperties() {
		return Map.of();
	}

}
