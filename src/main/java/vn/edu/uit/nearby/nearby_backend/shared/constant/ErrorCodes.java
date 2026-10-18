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

	public static final String INVALID_EMAIL = "INVALID_EMAIL";

	public static final String WEAK_PASSWORD = "WEAK_PASSWORD";

	public static final String REGISTRATION_REJECTED = "REGISTRATION_REJECTED";

	public static final String INVALID_VERIFICATION_TOKEN = "INVALID_VERIFICATION_TOKEN";

	public static final String INVALID_CREDENTIALS = "INVALID_CREDENTIALS";

	public static final String EMAIL_NOT_VERIFIED = "EMAIL_NOT_VERIFIED";

	public static final String ACCOUNT_LOCKED = "ACCOUNT_LOCKED";

	public static final String INVALID_REFRESH_TOKEN = "INVALID_REFRESH_TOKEN";

	public static final String REFRESH_TOKEN_REUSED = "REFRESH_TOKEN_REUSED";

	public static final String INVALID_PASSWORD_RESET_TOKEN = "INVALID_PASSWORD_RESET_TOKEN";

	public static final String USER_NOT_FOUND = "USER_NOT_FOUND";

	public static final String INVALID_IMAGE = "INVALID_IMAGE";

	public static final String INVALID_AVATAR_KEY = "INVALID_AVATAR_KEY";

	public static final String AVATAR_NOT_UPLOADED = "AVATAR_NOT_UPLOADED";

	public static final String OUTDATED_TERMS_VERSION = "OUTDATED_TERMS_VERSION";

	public static final String LOCATION_CONSENT_REQUIRED = "LOCATION_CONSENT_REQUIRED";

	public static final String CANNOT_LOCK_OWN_ACCOUNT = "CANNOT_LOCK_OWN_ACCOUNT";

	public static final String CANNOT_LOCK_ADMIN = "CANNOT_LOCK_ADMIN";

	public static final String ACCOUNT_ALREADY_LOCKED = "ACCOUNT_ALREADY_LOCKED";

	public static final String ACCOUNT_NOT_LOCKED = "ACCOUNT_NOT_LOCKED";

	private ErrorCodes() {
	}

}
