package vn.edu.uit.nearby.nearby_backend.identity.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasItems;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.RequestBuilder;
import org.springframework.test.web.servlet.ResultActions;

import vn.edu.uit.nearby.nearby_backend.identity.infrastructure.UserRepository;
import vn.edu.uit.nearby.nearby_backend.identity.web.AccountTestClient.Session;
import vn.edu.uit.nearby.nearby_backend.shared.security.TokenService;
import vn.edu.uit.nearby.nearby_backend.support.IntegrationTest;
import vn.edu.uit.nearby.nearby_backend.support.RecordingAccountMailer;

@IntegrationTest
class AuthApiIntegrationTests {

	private static final String REGISTER = "/api/v1/auth/register";

	private static final String VERIFY = "/api/v1/auth/email-verification";

	private static final String REFRESH = "/api/v1/auth/refresh";

	private static final String LOGOUT = "/api/v1/auth/logout";

	@Autowired
	MockMvc mvc;

	@Autowired
	RecordingAccountMailer mailer;

	@Autowired
	UserRepository users;

	@Autowired
	PasswordEncoder passwordEncoder;

	@Autowired
	TokenService tokenService;

	private AccountTestClient client;

	private final String email = AccountTestClient.uniqueEmail();

	@BeforeEach
	void createClient() {
		client = new AccountTestClient(mvc, users, passwordEncoder, tokenService);
	}

	@Test
	void newAccountMustVerifyItsEmailBeforeLoggingIn() throws Exception {
		register(email, "Secret123").andExpect(status().isCreated())
			.andExpect(jsonPath("$.email").value(email))
			.andExpect(jsonPath("$.status").value("PENDING"));
		client.login(email, "Secret123")
			.andExpect(status().isForbidden())
			.andExpect(jsonPath("$.code").value("EMAIL_NOT_VERIFIED"));

		client.postJson(VERIFY, tokenJson(verificationToken())).andExpect(status().isNoContent());

		client.login(email, "Secret123")
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.tokenType").value("Bearer"))
			.andExpect(jsonPath("$.accessToken").isNotEmpty())
			.andExpect(jsonPath("$.refreshToken").isNotEmpty());
	}

	@Test
	void verificationTokenWorksOnlyOnce() throws Exception {
		register(email, "Secret123");
		String token = verificationToken();
		client.postJson(VERIFY, tokenJson(token)).andExpect(status().isNoContent());

		client.postJson(VERIFY, tokenJson(token))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("INVALID_VERIFICATION_TOKEN"));
	}

	@Test
	void resendIssuesANewWorkingVerificationToken() throws Exception {
		register(email, "Secret123");
		String first = verificationToken();

		client.postJson("/api/v1/auth/email-verification/resend", emailJson(email)).andExpect(status().isAccepted());

		String second = verificationToken();
		assertThat(second).isNotEqualTo(first);
		client.postJson(VERIFY, tokenJson(second)).andExpect(status().isNoContent());
	}

	@Test
	void takenEmailGetsAGenericErrorThatDoesNotRevealTheAccount() throws Exception {
		client.activeUser(email);

		register(email.toUpperCase(), "Secret123").andExpect(status().isConflict())
			.andExpect(jsonPath("$.code").value("REGISTRATION_REJECTED"))
			.andExpect(jsonPath("$.detail").value("Registration could not be completed with the provided details."));
	}

	@Test
	void weakPasswordIsRejectedWithEveryReason() throws Exception {
		register(email, "short").andExpect(status().isBadRequest())
			.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
			.andExpect(jsonPath("$.code").value("WEAK_PASSWORD"))
			.andExpect(jsonPath("$.violations[*].rule").value(containsInAnyOrder("MIN_LENGTH", "LETTER_AND_DIGIT")));
	}

	@Test
	void malformedRegistrationIsAValidationError() throws Exception {
		client.postJson(REGISTER, """
				{"email":"not-an-email","password":"Secret123","displayName":""}""")
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
			.andExpect(jsonPath("$.errors[*].field").value(hasItems("email", "displayName")));
	}

	@Test
	void tooManyRegistrationsFromOneIpAreRateLimited() throws Exception {
		String sameIp = AccountTestClient.uniqueClientIp();
		for (int i = 0; i < 10; i++) {
			mvc.perform(registration(AccountTestClient.uniqueEmail(), sameIp)).andExpect(status().isCreated());
		}

		mvc.perform(registration(AccountTestClient.uniqueEmail(), sameIp))
			.andExpect(status().isTooManyRequests())
			.andExpect(header().exists(HttpHeaders.RETRY_AFTER));
	}

	@Test
	void wrongPasswordAndUnknownEmailLookTheSame() throws Exception {
		client.activeUser(email);

		MvcResult wrongPassword = client.login(email, "Wrong1234").andExpect(status().isUnauthorized()).andReturn();
		MvcResult unknownEmail = client.login(AccountTestClient.uniqueEmail(), "Wrong1234")
			.andExpect(status().isUnauthorized())
			.andReturn();

		assertThat(AccountTestClient.read(unknownEmail, "$.code")).isEqualTo("INVALID_CREDENTIALS");
		assertThat(AccountTestClient.read(unknownEmail, "$.detail"))
			.isEqualTo(AccountTestClient.read(wrongPassword, "$.detail"));
	}

	@Test
	void fiveFailedLoginsBlockLoginForTheEmailEvenWithTheRightPassword() throws Exception {
		client.activeUser(email);
		for (int i = 0; i < 5; i++) {
			client.login(email, "Wrong1234").andExpect(status().isUnauthorized());
		}

		client.login(email, AccountTestClient.PASSWORD)
			.andExpect(status().isTooManyRequests())
			.andExpect(jsonPath("$.code").value("RATE_LIMITED"))
			.andExpect(header().exists(HttpHeaders.RETRY_AFTER));
	}

	@Test
	void successfulLoginResetsTheFailureCount() throws Exception {
		client.activeUser(email);
		for (int i = 0; i < 4; i++) {
			client.login(email, "Wrong1234");
		}
		client.login(email, AccountTestClient.PASSWORD).andExpect(status().isOk());

		for (int i = 0; i < 4; i++) {
			client.login(email, "Wrong1234");
		}
		client.login(email, AccountTestClient.PASSWORD).andExpect(status().isOk());
	}

	@Test
	void accessTokenOpensProtectedEndpoints() throws Exception {
		client.activeUser(email);
		Session session = client.loginSession(email);

		mvc.perform(get("/api/v1/users/me").header(HttpHeaders.AUTHORIZATION, session.bearer()))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.email").value(email));
	}

	@Test
	void refreshRotatesTheTokenAndReusingTheOldOneRevokesTheWholeChain() throws Exception {
		client.activeUser(email);
		Session session = client.loginSession(email);

		MvcResult rotated = client.postJson(REFRESH, refreshJson(session.refreshToken()))
			.andExpect(status().isOk())
			.andReturn();
		String newRefreshToken = AccountTestClient.read(rotated, "$.refreshToken");
		assertThat(newRefreshToken).isNotEqualTo(session.refreshToken());

		client.postJson(REFRESH, refreshJson(session.refreshToken()))
			.andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.code").value("REFRESH_TOKEN_REUSED"));
		client.postJson(REFRESH, refreshJson(newRefreshToken))
			.andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.code").value("REFRESH_TOKEN_REUSED"));
	}

	@Test
	void unknownRefreshTokenIsRejected() throws Exception {
		client.postJson(REFRESH, refreshJson("not-a-real-token"))
			.andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.code").value("INVALID_REFRESH_TOKEN"));
	}

	@Test
	void logoutRevokesTheRefreshTokenAndIsIdempotent() throws Exception {
		client.activeUser(email);
		Session session = client.loginSession(email);

		client.postJson(LOGOUT, refreshJson(session.refreshToken())).andExpect(status().isNoContent());
		client.postJson(LOGOUT, refreshJson(session.refreshToken())).andExpect(status().isNoContent());
		client.postJson(LOGOUT, refreshJson("unknown-token")).andExpect(status().isNoContent());

		client.postJson(REFRESH, refreshJson(session.refreshToken())).andExpect(status().isUnauthorized());
	}

	@Test
	void forgotPasswordAnswersTheSameForUnknownEmails() throws Exception {
		client.postJson("/api/v1/auth/password/forgot", emailJson(AccountTestClient.uniqueEmail()))
			.andExpect(status().isAccepted());
	}

	@Test
	void passwordResetSetsTheNewPasswordAndLogsOutEverySession() throws Exception {
		client.activeUser(email);
		Session session = client.loginSession(email);
		client.postJson("/api/v1/auth/password/forgot", emailJson(email)).andExpect(status().isAccepted());
		String resetToken = mailer.passwordResetTokenFor(email).orElseThrow();

		client.postJson("/api/v1/auth/password/reset", """
				{"token":"%s","newPassword":"NewSecret456"}""".formatted(resetToken)).andExpect(status().isNoContent());

		client.login(email, AccountTestClient.PASSWORD).andExpect(status().isUnauthorized());
		client.login(email, "NewSecret456").andExpect(status().isOk());
		client.postJson(REFRESH, refreshJson(session.refreshToken())).andExpect(status().isUnauthorized());
		client.postJson("/api/v1/auth/password/reset", """
				{"token":"%s","newPassword":"Another789"}""".formatted(resetToken))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("INVALID_PASSWORD_RESET_TOKEN"));
	}

	private ResultActions register(String email, String password) throws Exception {
		return client.postJson(REGISTER, """
				{"email":"%s","password":"%s","displayName":"An Nguyen"}""".formatted(email, password));
	}

	private static RequestBuilder registration(String email, String clientIp) {
		return post(REGISTER).contentType(MediaType.APPLICATION_JSON).content("""
				{"email":"%s","password":"Secret123","displayName":"An Nguyen"}""".formatted(email)).with(request -> {
			request.setRemoteAddr(clientIp);
			return request;
		});
	}

	private String verificationToken() {
		return mailer.verificationTokenFor(email).orElseThrow();
	}

	private static String tokenJson(String token) {
		return "{\"token\":\"" + token + "\"}";
	}

	private static String emailJson(String email) {
		return "{\"email\":\"" + email + "\"}";
	}

	private static String refreshJson(String refreshToken) {
		return "{\"refreshToken\":\"" + refreshToken + "\"}";
	}

}
