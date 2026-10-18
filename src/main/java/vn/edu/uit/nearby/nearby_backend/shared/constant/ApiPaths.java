package vn.edu.uit.nearby.nearby_backend.shared.constant;

public final class ApiPaths {

	public static final String API_PATTERN = "/api/**";

	public static final String API_V1 = "/api/v1";

	public static final String AUTH_BASE = API_V1 + "/auth";

	public static final String AUTH_PATTERN = AUTH_BASE + "/**";

	public static final String CURRENT_USER_BASE = API_V1 + "/users/me";

	public static final String ADMIN_BASE = API_V1 + "/admin";

	public static final String ADMIN_PATTERN = ADMIN_BASE + "/**";

	public static final String ADMIN_USERS_BASE = ADMIN_BASE + "/users";

	public static final String TEST_BASE = "/api/test";

	public static final String TEST_PATTERN = TEST_BASE + "/**";

	public static final String WEBSOCKET_ENDPOINT = "/ws";

	public static final String WEBSOCKET_PATTERN = WEBSOCKET_ENDPOINT + "/**";

	public static final String ERROR = "/error";

	public static final String ACTUATOR_PATTERN = "/actuator/**";

	public static final String ACTUATOR_HEALTH_PATTERN = "/actuator/health/**";

	public static final String ACTUATOR_INFO = "/actuator/info";

	public static final String ACTUATOR_PROMETHEUS = "/actuator/prometheus";

	public static final String OPENAPI_DOCS_PATTERN = "/v3/api-docs/**";

	public static final String SWAGGER_UI_PATTERN = "/swagger-ui/**";

	public static final String SWAGGER_UI_PAGE = "/swagger-ui.html";

	private ApiPaths() {
	}

}
