package vn.edu.uit.nearby.nearby_backend.shared.constant;

public final class ErrorCodes {

	public static final String PROBLEM_TYPE_BASE = "https://nearby.uit.edu.vn/problems/";

	public static final String UNAUTHENTICATED = "UNAUTHENTICATED";

	public static final String ACCESS_DENIED = "ACCESS_DENIED";

	public static final String VALIDATION_FAILED = "VALIDATION_FAILED";

	public static final String CONCURRENT_MODIFICATION = "CONCURRENT_MODIFICATION";

	public static final String DATA_CONFLICT = "DATA_CONFLICT";

	public static final String INTERNAL_ERROR = "INTERNAL_ERROR";

	public static final String RATE_LIMITED = "RATE_LIMITED";

	public static final String INVALID_CURSOR = "INVALID_CURSOR";

	public static final String IDEMPOTENCY_KEY_REQUIRED = "IDEMPOTENCY_KEY_REQUIRED";

	public static final String DUPLICATE_REQUEST = "DUPLICATE_REQUEST";

	private ErrorCodes() {
	}

}
