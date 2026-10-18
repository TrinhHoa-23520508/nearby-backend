package vn.edu.uit.nearby.nearby_backend.shared.error;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import vn.edu.uit.nearby.nearby_backend.shared.constant.ErrorCodes;
import vn.edu.uit.nearby.nearby_backend.shared.constant.Roles;
import vn.edu.uit.nearby.nearby_backend.shared.constant.SecurityConstants;
import vn.edu.uit.nearby.nearby_backend.shared.security.TokenService;
import vn.edu.uit.nearby.nearby_backend.support.IntegrationTest;

@IntegrationTest
class ErrorHandlingIntegrationTests {

	@Autowired
	MockMvc mvc;

	@Autowired
	TokenService tokenService;

	private String userToken;

	@BeforeEach
	void issueUserToken() {
		userToken = bearer(Roles.USER);
	}

	@ParameterizedTest
	@CsvSource({ "not-found,404,LISTING_NOT_FOUND", "conflict,409,OFFER_ALREADY_OPEN", "invalid,400,INVALID_PRICE",
			"forbidden,403,NOT_LISTING_OWNER", "rate-limited,429,RATE_LIMITED" })
	void businessExceptionsMapToProblemDetails(String type, int expectedStatus, String expectedCode) throws Exception {
		mvc.perform(get("/api/v1/probe/errors/" + type).header(HttpHeaders.AUTHORIZATION, userToken))
			.andExpect(status().is(expectedStatus))
			.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
			.andExpect(jsonPath("$.status").value(expectedStatus))
			.andExpect(jsonPath("$.code").value(expectedCode))
			.andExpect(jsonPath("$.type")
				.value(ErrorCodes.PROBLEM_TYPE_BASE + expectedCode.toLowerCase().replace('_', '-')));
	}

	@Test
	void unexpectedExceptionHidesInternalDetails() throws Exception {
		mvc.perform(get("/api/v1/probe/errors/boom").header(HttpHeaders.AUTHORIZATION, userToken))
			.andExpect(status().isInternalServerError())
			.andExpect(jsonPath("$.code").value("INTERNAL_ERROR"))
			.andExpect(jsonPath("$.detail").value("An unexpected error occurred."));
	}

	@Test
	void invalidRequestBodyListsTheOffendingFields() throws Exception {
		mvc.perform(post("/api/v1/probe/validated").header(HttpHeaders.AUTHORIZATION, userToken)
			.contentType(MediaType.APPLICATION_JSON)
			.content("{\"name\":\"\"}"))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
			.andExpect(jsonPath("$.errors[0].field").value("name"));
	}

	@Test
	void missingTokenReturnsUnauthorizedProblemWithBearerChallenge() throws Exception {
		mvc.perform(get("/api/v1/probe/errors/not-found"))
			.andExpect(status().isUnauthorized())
			.andExpect(header().string(HttpHeaders.WWW_AUTHENTICATE, SecurityConstants.BEARER_SCHEME))
			.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
			.andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
	}

	@Test
	void invalidTokenReturnsUnauthorizedProblem() throws Exception {
		mvc.perform(get("/api/v1/probe/errors/not-found").header(HttpHeaders.AUTHORIZATION,
				SecurityConstants.BEARER_PREFIX + "not-a-jwt"))
			.andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
	}

	@Test
	void userRoleCannotReachAdminEndpoints() throws Exception {
		mvc.perform(get("/api/v1/admin/reports").header(HttpHeaders.AUTHORIZATION, userToken))
			.andExpect(status().isForbidden())
			.andExpect(jsonPath("$.code").value("ACCESS_DENIED"));
	}

	@Test
	void healthEndpointIsPublic() throws Exception {
		mvc.perform(get("/actuator/health")).andExpect(status().isOk());
	}

	private String bearer(String role) {
		return SecurityConstants.BEARER_PREFIX + tokenService.issueAccessToken(UUID.randomUUID(), role).value();
	}

}
