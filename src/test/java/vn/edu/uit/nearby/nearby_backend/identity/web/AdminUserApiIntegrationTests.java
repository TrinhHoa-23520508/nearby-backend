package vn.edu.uit.nearby.nearby_backend.identity.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;

import com.jayway.jsonpath.JsonPath;

import vn.edu.uit.nearby.nearby_backend.identity.domain.AuditAction;
import vn.edu.uit.nearby.nearby_backend.identity.domain.AuditLogEntry;
import vn.edu.uit.nearby.nearby_backend.identity.domain.User;
import vn.edu.uit.nearby.nearby_backend.identity.infrastructure.AuditLogRepository;
import vn.edu.uit.nearby.nearby_backend.identity.infrastructure.UserRepository;
import vn.edu.uit.nearby.nearby_backend.identity.web.AccountTestClient.Session;
import vn.edu.uit.nearby.nearby_backend.shared.security.TokenService;
import vn.edu.uit.nearby.nearby_backend.support.IntegrationTest;

@IntegrationTest
class AdminUserApiIntegrationTests {

	private static final String ADMIN_USERS = "/api/v1/admin/users";

	private static final String LOCK_REASON = "{\"reason\":\"Repeated scam reports\"}";

	@Autowired
	MockMvc mvc;

	@Autowired
	UserRepository users;

	@Autowired
	AuditLogRepository auditLog;

	@Autowired
	PasswordEncoder passwordEncoder;

	@Autowired
	TokenService tokenService;

	private AccountTestClient client;

	private User admin;

	private String adminBearer;

	@BeforeEach
	void createAdmin() {
		client = new AccountTestClient(mvc, users, passwordEncoder, tokenService);
		admin = client.admin();
		adminBearer = client.bearerFor(admin);
	}

	@Test
	void regularUsersCannotUseAdminEndpoints() throws Exception {
		User user = client.activeUser(AccountTestClient.uniqueEmail());

		mvc.perform(get(ADMIN_USERS).header(HttpHeaders.AUTHORIZATION, client.bearerFor(user)))
			.andExpect(status().isForbidden())
			.andExpect(jsonPath("$.code").value("ACCESS_DENIED"));
	}

	@Test
	void lockingTakesEffectImmediatelyAndIsAudited() throws Exception {
		String email = AccountTestClient.uniqueEmail();
		User user = client.activeUser(email);
		Session session = client.loginSession(email);

		lock(user.getId()).andExpect(status().isOk()).andExpect(jsonPath("$.status").value("BANNED"));

		mvc.perform(get("/api/v1/users/me").header(HttpHeaders.AUTHORIZATION, session.bearer()))
			.andExpect(status().isUnauthorized());
		client.postJson("/api/v1/auth/refresh", "{\"refreshToken\":\"" + session.refreshToken() + "\"}")
			.andExpect(status().isUnauthorized());
		client.login(email, AccountTestClient.PASSWORD)
			.andExpect(status().isForbidden())
			.andExpect(jsonPath("$.code").value("ACCOUNT_LOCKED"));
		List<AuditLogEntry> entries = auditLog.findByTargetIdOrderByIdAsc(user.getId());
		assertThat(entries).singleElement().satisfies(entry -> {
			assertThat(entry.getAction()).isEqualTo(AuditAction.USER_LOCKED);
			assertThat(entry.getActorId()).isEqualTo(admin.getId());
			assertThat(entry.getDetail()).contains("Repeated scam reports");
		});
	}

	@Test
	void unlockingLetsTheUserLogInAgain() throws Exception {
		String email = AccountTestClient.uniqueEmail();
		User user = client.activeUser(email);
		lock(user.getId());

		client.postJson(ADMIN_USERS + "/" + user.getId() + "/unlock", "{\"reason\":\"Appeal accepted\"}", adminBearer)
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.status").value("ACTIVE"));

		Session session = client.loginSession(email);
		mvc.perform(get("/api/v1/users/me").header(HttpHeaders.AUTHORIZATION, session.bearer()))
			.andExpect(status().isOk());
		assertThat(auditLog.findByTargetIdOrderByIdAsc(user.getId())).extracting(AuditLogEntry::getAction)
			.containsExactly(AuditAction.USER_LOCKED, AuditAction.USER_UNLOCKED);
	}

	@Test
	void lockingTwiceIsAConflict() throws Exception {
		User user = client.activeUser(AccountTestClient.uniqueEmail());
		lock(user.getId());

		lock(user.getId()).andExpect(status().isConflict())
			.andExpect(jsonPath("$.code").value("ACCOUNT_ALREADY_LOCKED"));
	}

	@Test
	void adminsCannotBeLocked() throws Exception {
		lock(admin.getId()).andExpect(status().isForbidden())
			.andExpect(jsonPath("$.code").value("CANNOT_LOCK_OWN_ACCOUNT"));
		lock(client.admin().getId()).andExpect(status().isForbidden())
			.andExpect(jsonPath("$.code").value("CANNOT_LOCK_ADMIN"));
	}

	@Test
	void lockingAnUnknownUserIsNotFound() throws Exception {
		lock(UUID.randomUUID()).andExpect(status().isNotFound()).andExpect(jsonPath("$.code").value("USER_NOT_FOUND"));
	}

	@Test
	void lockRequiresAReason() throws Exception {
		User user = client.activeUser(AccountTestClient.uniqueEmail());

		client.postJson(ADMIN_USERS + "/" + user.getId() + "/lock", "{\"reason\":\"\"}", adminBearer)
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
	}

	@Test
	void singleUserIsShownToAdmins() throws Exception {
		User user = client.activeUser(AccountTestClient.uniqueEmail());

		mvc.perform(get(ADMIN_USERS + "/" + user.getId()).header(HttpHeaders.AUTHORIZATION, adminBearer))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.email").value(user.getEmail()))
			.andExpect(jsonPath("$.emailVerified").value(true));
	}

	@Test
	void searchFindsUsersByEmailAndPagesNewestFirst() throws Exception {
		String marker = UUID.randomUUID().toString().substring(0, 8);
		User oldest = client.activeUser("first-" + marker + "@example.com");
		User middle = client.activeUser("second-" + marker + "@example.com");
		User newest = client.activeUser("third-" + marker + "@example.com");

		MvcResult firstPage = search("?query=" + marker.toUpperCase() + "&limit=2").andExpect(status().isOk())
			.andExpect(jsonPath("$.hasMore").value(true))
			.andReturn();
		List<String> firstIds = JsonPath.read(firstPage.getResponse().getContentAsString(), "$.items[*].id");
		String cursor = AccountTestClient.read(firstPage, "$.nextCursor");

		MvcResult secondPage = search("?query=" + marker + "&limit=2&cursor=" + cursor).andExpect(status().isOk())
			.andExpect(jsonPath("$.hasMore").value(false))
			.andReturn();
		List<String> secondIds = JsonPath.read(secondPage.getResponse().getContentAsString(), "$.items[*].id");

		assertThat(firstIds).containsExactly(newest.getId().toString(), middle.getId().toString());
		assertThat(secondIds).containsExactly(oldest.getId().toString());
	}

	@Test
	void searchFiltersByStatus() throws Exception {
		String marker = UUID.randomUUID().toString().substring(0, 8);
		client.activeUser("active-" + marker + "@example.com");
		User locked = client.activeUser("locked-" + marker + "@example.com");
		lock(locked.getId());

		search("?query=" + marker + "&status=BANNED").andExpect(status().isOk())
			.andExpect(jsonPath("$.items.length()").value(1))
			.andExpect(jsonPath("$.items[0].id").value(locked.getId().toString()));
	}

	@Test
	void searchTreatsLikeWildcardsLiterally() throws Exception {
		search("?query=%25").andExpect(status().isOk()).andExpect(jsonPath("$.items.length()").value(0));
	}

	@Test
	void invalidCursorIsRejected() throws Exception {
		search("?cursor=not-a-cursor").andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("INVALID_CURSOR"));
	}

	private ResultActions lock(UUID userId) throws Exception {
		return client.postJson(ADMIN_USERS + "/" + userId + "/lock", LOCK_REASON, adminBearer);
	}

	private ResultActions search(String query) throws Exception {
		return mvc.perform(get(ADMIN_USERS + query).header(HttpHeaders.AUTHORIZATION, adminBearer));
	}

}
