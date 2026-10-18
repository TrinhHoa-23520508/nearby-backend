package vn.edu.uit.nearby.nearby_backend.shared.constant;

public final class SecurityConstants {

	public static final String BEARER_SCHEME = "Bearer";

	public static final String BEARER_PREFIX = BEARER_SCHEME + " ";

	public static final String OPENAPI_BEARER_SCHEME = "bearerAuth";

	public static final String ROLES_CLAIM = "roles";

	public static final String AUTHORITY_PREFIX = "ROLE_";

	public static final int REFRESH_TOKEN_BYTES = 32;

	private SecurityConstants() {
	}

}
