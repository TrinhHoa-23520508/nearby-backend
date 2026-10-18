package vn.edu.uit.nearby.nearby_backend.identity.web;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import com.jayway.jsonpath.JsonPath;

import vn.edu.uit.nearby.nearby_backend.identity.domain.EmailAddress;
import vn.edu.uit.nearby.nearby_backend.identity.domain.User;
import vn.edu.uit.nearby.nearby_backend.identity.infrastructure.UserRepository;
import vn.edu.uit.nearby.nearby_backend.shared.constant.SecurityConstants;
import vn.edu.uit.nearby.nearby_backend.shared.security.TokenService;

/**
 * Creates accounts and sessions for API tests. Every request gets its own client IP so
 * the per-IP registration limit never couples tests together.
 */
final class AccountTestClient {

	static final String PASSWORD = "Secret123";

	private static final AtomicInteger IP_SUFFIX = new AtomicInteger();

	private final MockMvc mvc;

	private final UserRepository users;

	private final PasswordEncoder passwordEncoder;

	private final TokenService tokenService;

	AccountTestClient(MockMvc mvc, UserRepository users, PasswordEncoder passwordEncoder, TokenService tokenService) {
		this.mvc = mvc;
		this.users = users;
		this.passwordEncoder = passwordEncoder;
		this.tokenService = tokenService;
	}

	static String uniqueEmail() {
		return "user-" + UUID.randomUUID() + "@example.com";
	}

	static String uniqueClientIp() {
		int suffix = IP_SUFFIX.incrementAndGet();
		return "10.%d.%d.%d".formatted((suffix >> 16) & 255, (suffix >> 8) & 255, suffix & 255);
	}

	User activeUser(String email) {
		User user = User.register(EmailAddress.of(email), passwordEncoder.encode(PASSWORD), "Test User", Instant.now());
		user.verifyEmail(Instant.now());
		return users.save(user);
	}

	User admin() {
		return users.save(User.administrator(EmailAddress.of(uniqueEmail()), passwordEncoder.encode(PASSWORD),
				"Test Admin", Instant.now()));
	}

	String bearerFor(User user) {
		return SecurityConstants.BEARER_PREFIX
				+ tokenService.issueAccessToken(user.getId(), user.getRole().name()).value();
	}

	ResultActions postJson(String path, String json) throws Exception {
		return mvc.perform(jsonRequest(post(path), json));
	}

	ResultActions postJson(String path, String json, String bearer) throws Exception {
		return mvc.perform(jsonRequest(post(path), json).header(HttpHeaders.AUTHORIZATION, bearer));
	}

	ResultActions login(String email, String password) throws Exception {
		return postJson("/api/v1/auth/login", """
				{"email":"%s","password":"%s"}""".formatted(email, password));
	}

	Session loginSession(String email) throws Exception {
		MvcResult result = login(email, PASSWORD).andReturn();
		return new Session(read(result, "$.accessToken"), read(result, "$.refreshToken"));
	}

	static MockHttpServletRequestBuilder jsonRequest(MockHttpServletRequestBuilder request, String json) {
		String clientIp = uniqueClientIp();
		return request.contentType(MediaType.APPLICATION_JSON).content(json).with(servletRequest -> {
			servletRequest.setRemoteAddr(clientIp);
			return servletRequest;
		});
	}

	static String read(MvcResult result, String jsonPath) throws Exception {
		return JsonPath.read(result.getResponse().getContentAsString(), jsonPath);
	}

	record Session(String accessToken, String refreshToken) {

		String bearer() {
			return SecurityConstants.BEARER_PREFIX + accessToken;
		}

	}

}
