package vn.edu.uit.nearby.nearby_backend.identity.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import vn.edu.uit.nearby.nearby_backend.identity.LocationConsents;
import vn.edu.uit.nearby.nearby_backend.identity.domain.User;
import vn.edu.uit.nearby.nearby_backend.identity.infrastructure.UserRepository;
import vn.edu.uit.nearby.nearby_backend.shared.security.TokenService;
import vn.edu.uit.nearby.nearby_backend.support.InMemoryImageStorage;
import vn.edu.uit.nearby.nearby_backend.support.IntegrationTest;

@IntegrationTest
class ProfileApiIntegrationTests {

	private static final String ME = "/api/v1/users/me";

	private static final String AVATAR = ME + "/avatar";

	private static final String CONSENT = ME + "/location-consent";

	@Autowired
	MockMvc mvc;

	@Autowired
	UserRepository users;

	@Autowired
	PasswordEncoder passwordEncoder;

	@Autowired
	TokenService tokenService;

	@Autowired
	InMemoryImageStorage storage;

	@Autowired
	LocationConsents locationConsents;

	private User user;

	private String bearer;

	private AccountTestClient client;

	@BeforeEach
	void createUser() {
		client = new AccountTestClient(mvc, users, passwordEncoder, tokenService);
		user = client.activeUser(AccountTestClient.uniqueEmail());
		bearer = client.bearerFor(user);
	}

	@Test
	void profileRequiresAnAccessToken() throws Exception {
		mvc.perform(get(ME))
			.andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
	}

	@Test
	void profileShowsTheAccount() throws Exception {
		mvc.perform(get(ME).header(HttpHeaders.AUTHORIZATION, bearer))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.id").value(user.getId().toString()))
			.andExpect(jsonPath("$.role").value("USER"))
			.andExpect(jsonPath("$.status").value("ACTIVE"))
			.andExpect(jsonPath("$.emailVerified").value(true))
			.andExpect(jsonPath("$.avatarUrl").doesNotExist())
			.andExpect(jsonPath("$.passwordHash").doesNotExist());
	}

	@Test
	void updatedHomeAreaIsStoredRounded() throws Exception {
		updateProfile("""
				{"displayName":"  Binh Tran ","homeArea":{"latitude":10.870149,"longitude":106.803151}}""")
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.displayName").value("Binh Tran"))
			.andExpect(jsonPath("$.homeArea.latitude").value(10.87))
			.andExpect(jsonPath("$.homeArea.longitude").value(106.803));

		mvc.perform(get(ME).header(HttpHeaders.AUTHORIZATION, bearer))
			.andExpect(jsonPath("$.homeArea.longitude").value(106.803));
	}

	@Test
	void omittingTheHomeAreaClearsIt() throws Exception {
		updateProfile("""
				{"displayName":"An","homeArea":{"latitude":10.87,"longitude":106.8}}""");

		updateProfile("""
				{"displayName":"An"}""").andExpect(status().isOk()).andExpect(jsonPath("$.homeArea").doesNotExist());
	}

	@Test
	void outOfRangeCoordinatesAreAValidationError() throws Exception {
		updateProfile("""
				{"displayName":"An","homeArea":{"latitude":91,"longitude":106.8}}""").andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.errors[0].field").value("homeArea.latitude"));
	}

	@Test
	void avatarIsUploadedWithAPresignedUrlThenConfirmed() throws Exception {
		String objectKey = requestUploadUrl("image/png", 2048);
		storage.simulateUpload(objectKey, "image/png", 2048);

		confirmAvatar(objectKey).andExpect(status().isOk())
			.andExpect(jsonPath("$.avatarUrl").value(containsString(objectKey)));
	}

	@Test
	void replacingTheAvatarDeletesThePreviousImageAfterCommit() throws Exception {
		String first = uploadedAvatar();
		String second = uploadedAvatar();

		confirmAvatar(second).andExpect(status().isOk());

		assertThat(storage.contains(first)).isFalse();
		assertThat(storage.contains(second)).isTrue();
	}

	@Test
	void removingTheAvatarDeletesTheImage() throws Exception {
		String objectKey = uploadedAvatar();

		mvc.perform(delete(AVATAR).header(HttpHeaders.AUTHORIZATION, bearer)).andExpect(status().isNoContent());

		assertThat(storage.contains(objectKey)).isFalse();
		mvc.perform(get(ME).header(HttpHeaders.AUTHORIZATION, bearer))
			.andExpect(jsonPath("$.avatarUrl").doesNotExist());
	}

	@Test
	void unsupportedFormatGetsNoUploadUrl() throws Exception {
		client.postJson(AVATAR + "/upload-url", """
				{"contentType":"image/gif","sizeBytes":1024}""", bearer)
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("INVALID_IMAGE"));
	}

	@Test
	void imageLargerThanFiveMegabytesGetsNoUploadUrl() throws Exception {
		client.postJson(AVATAR + "/upload-url", """
				{"contentType":"image/jpeg","sizeBytes":5242881}""", bearer)
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("INVALID_IMAGE"));
	}

	@Test
	void uploadThatTurnsOutTooLargeIsDeletedAndRejected() throws Exception {
		String objectKey = requestUploadUrl("image/jpeg", 1024);
		storage.simulateUpload(objectKey, "image/jpeg", 6L * 1024 * 1024);

		confirmAvatar(objectKey).andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("INVALID_IMAGE"));

		assertThat(storage.contains(objectKey)).isFalse();
	}

	@Test
	void confirmingSomeoneElsesKeyIsRejected() throws Exception {
		User other = client.activeUser(AccountTestClient.uniqueEmail());
		String othersKey = "avatars/" + other.getId() + "/0b6c7a3e-8a64-4c4c-9a55-0e1f0c3b9e11.jpg";

		confirmAvatar(othersKey).andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("INVALID_AVATAR_KEY"));
	}

	@Test
	void confirmingBeforeUploadingIsRejected() throws Exception {
		confirmAvatar(requestUploadUrl("image/jpeg", 1024)).andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("AVATAR_NOT_UPLOADED"));
	}

	@Test
	void consentLifecycleIsRecordedAndVisibleToOtherModules() throws Exception {
		mvc.perform(get(CONSENT).header(HttpHeaders.AUTHORIZATION, bearer))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.granted").value(false))
			.andExpect(jsonPath("$.currentTermsVersion").value("1.0"));

		mvc.perform(put(CONSENT).header(HttpHeaders.AUTHORIZATION, bearer)
			.contentType("application/json")
			.content("{\"termsVersion\":\"1.0\"}"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.granted").value(true))
			.andExpect(jsonPath("$.termsVersion").value("1.0"))
			.andExpect(jsonPath("$.consentedAt").isNotEmpty());
		assertThat(locationConsents.hasConsented(user.getId())).isTrue();

		mvc.perform(delete(CONSENT).header(HttpHeaders.AUTHORIZATION, bearer)).andExpect(status().isNoContent());
		mvc.perform(get(CONSENT).header(HttpHeaders.AUTHORIZATION, bearer))
			.andExpect(jsonPath("$.granted").value(false))
			.andExpect(jsonPath("$.withdrawnAt").isNotEmpty());
		assertThat(locationConsents.hasConsented(user.getId())).isFalse();
	}

	@Test
	void consentToAnOutdatedTermsVersionIsRejected() throws Exception {
		mvc.perform(put(CONSENT).header(HttpHeaders.AUTHORIZATION, bearer)
			.contentType("application/json")
			.content("{\"termsVersion\":\"0.9\"}"))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("OUTDATED_TERMS_VERSION"));
	}

	private ResultActions updateProfile(String json) throws Exception {
		return mvc.perform(AccountTestClient.jsonRequest(put(ME), json).header(HttpHeaders.AUTHORIZATION, bearer));
	}

	private String requestUploadUrl(String contentType, long sizeBytes) throws Exception {
		return AccountTestClient.read(client.postJson(AVATAR + "/upload-url", """
				{"contentType":"%s","sizeBytes":%d}""".formatted(contentType, sizeBytes), bearer)
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.method").value("PUT"))
			.andExpect(jsonPath("$.requiredHeaders['Content-Type']").value(contentType))
			.andReturn(), "$.objectKey");
	}

	private String uploadedAvatar() throws Exception {
		String objectKey = requestUploadUrl("image/jpeg", 1024);
		storage.simulateUpload(objectKey, "image/jpeg", 1024);
		confirmAvatar(objectKey).andExpect(status().isOk());
		return objectKey;
	}

	private ResultActions confirmAvatar(String objectKey) throws Exception {
		return mvc.perform(AccountTestClient.jsonRequest(put(AVATAR), "{\"objectKey\":\"" + objectKey + "\"}")
			.header(HttpHeaders.AUTHORIZATION, bearer));
	}

}
