package vn.edu.uit.nearby.nearby_backend.identity.domain.password;

import java.io.Serial;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import vn.edu.uit.nearby.nearby_backend.shared.constant.ErrorCodes;
import vn.edu.uit.nearby.nearby_backend.shared.error.InvalidRequestException;

public class WeakPasswordException extends InvalidRequestException {

	@Serial
	private static final long serialVersionUID = 1L;

	private final List<PasswordViolation> violations;

	public WeakPasswordException(List<PasswordViolation> violations) {
		super(ErrorCodes.WEAK_PASSWORD,
				violations.stream().map(PasswordViolation::message).collect(Collectors.joining(" ")));
		this.violations = List.copyOf(violations);
	}

	public List<PasswordViolation> getViolations() {
		return violations;
	}

	@Override
	public Map<String, Object> getProperties() {
		return Map.of("violations", violations);
	}

}
