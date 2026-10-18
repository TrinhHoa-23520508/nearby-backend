package vn.edu.uit.nearby.nearby_backend.shared.web.idempotency;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import vn.edu.uit.nearby.nearby_backend.shared.constant.ApiLimits;
import vn.edu.uit.nearby.nearby_backend.shared.constant.HeaderNames;
import vn.edu.uit.nearby.nearby_backend.shared.constant.Roles;
import vn.edu.uit.nearby.nearby_backend.shared.constant.SecurityConstants;
import vn.edu.uit.nearby.nearby_backend.shared.security.TokenService;
import vn.edu.uit.nearby.nearby_backend.support.IntegrationTest;

@IntegrationTest
class IdempotencyIntegrationTests {

	private static final String CREATE = "/api/v1/probe/idempotent";

	private static final String FAILING = "/api/v1/probe/idempotent-failing";

	@Autowired
	MockMvc mvc;

	@Autowired
	TokenService tokenService;

	private String userToken;

	@BeforeEach
	void issueUserToken() {
		userToken = bearer(Roles.USER);
	}

	@Test
	void firstRequestWithKeyIsProcessed() throws Exception {
		send(CREATE, UUID.randomUUID().toString()).andExpect(status().isOk());
	}

	@Test
	void repeatedKeyIsRejectedAsDuplicate() throws Exception {
		String key = UUID.randomUUID().toString();
		send(CREATE, key);

		send(CREATE, key).andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("DUPLICATE_REQUEST"));
	}

	@Test
	void sameKeyFromAnotherUserIsIndependent() throws Exception {
		String key = UUID.randomUUID().toString();
		send(CREATE, key);

		mvc.perform(post(CREATE).header(HttpHeaders.AUTHORIZATION, bearer(Roles.USER))
			.header(HeaderNames.IDEMPOTENCY_KEY, key)).andExpect(status().isOk());
	}

	@Test
	void missingKeyIsRejected() throws Exception {
		mvc.perform(post(CREATE).header(HttpHeaders.AUTHORIZATION, userToken))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("IDEMPOTENCY_KEY_REQUIRED"));
	}

	@Test
	void keyAtMaximumLengthIsAccepted() throws Exception {
		send(CREATE, "k".repeat(ApiLimits.IDEMPOTENCY_KEY_MAX_LENGTH - 36) + UUID.randomUUID())
			.andExpect(status().isOk());
	}

	@Test
	void keyLongerThanMaximumIsRejected() throws Exception {
		send(CREATE, "k".repeat(ApiLimits.IDEMPOTENCY_KEY_MAX_LENGTH + 1)).andExpect(status().isBadRequest());
	}

	@Test
	void failedRequestReleasesTheKeySoTheClientCanRetry() throws Exception {
		String key = UUID.randomUUID().toString();
		send(FAILING, key).andExpect(status().isInternalServerError());

		send(FAILING, key).andExpect(status().isInternalServerError());
	}

	private String bearer(String role) {
		return SecurityConstants.BEARER_PREFIX + tokenService.issueAccessToken(UUID.randomUUID(), role).value();
	}

	private ResultActions send(String path, String idempotencyKey) throws Exception {
		return mvc.perform(post(path).header(HttpHeaders.AUTHORIZATION, userToken)
			.header(HeaderNames.IDEMPOTENCY_KEY, idempotencyKey));
	}

}
