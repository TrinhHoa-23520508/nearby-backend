package vn.edu.uit.nearby.nearby_backend.identity.application.admin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static vn.edu.uit.nearby.nearby_backend.identity.domain.UserFixtures.NOW;

import java.time.Clock;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.jpa.domain.Specification;

import vn.edu.uit.nearby.nearby_backend.identity.UserBanned;
import vn.edu.uit.nearby.nearby_backend.identity.UserUnbanned;
import vn.edu.uit.nearby.nearby_backend.identity.application.auth.UserSessions;
import vn.edu.uit.nearby.nearby_backend.identity.domain.AccountStatus;
import vn.edu.uit.nearby.nearby_backend.identity.domain.AuditAction;
import vn.edu.uit.nearby.nearby_backend.identity.domain.User;
import vn.edu.uit.nearby.nearby_backend.identity.domain.UserFixtures;
import vn.edu.uit.nearby.nearby_backend.identity.infrastructure.UserRepository;
import vn.edu.uit.nearby.nearby_backend.shared.error.BusinessException;
import vn.edu.uit.nearby.nearby_backend.shared.error.ConflictException;
import vn.edu.uit.nearby.nearby_backend.shared.error.OperationNotAllowedException;
import vn.edu.uit.nearby.nearby_backend.shared.web.CursorPage;

@ExtendWith(MockitoExtension.class)
class UserAdministrationServiceImplTests {

	private static final UUID ADMIN_ID = UUID.randomUUID();

	private static final String REASON = "Repeated scam reports";

	@Mock
	UserRepository users;

	@Mock
	UserSessions sessions;

	@Mock
	AuditLogService auditLog;

	@Mock
	ApplicationEventPublisher events;

	private UserAdministrationServiceImpl administration;

	@BeforeEach
	void createService() {
		administration = new UserAdministrationServiceImpl(users, sessions, auditLog, events,
				Clock.fixed(NOW, ZoneOffset.UTC));
	}

	@Test
	void lockingBansTheAccountEndsItsSessionsAuditsAndNotifiesOtherModules() {
		User user = givenUser(UserFixtures.activeUser());

		AdminUserView view = administration.lockUser(ADMIN_ID, user.getId(), REASON);

		assertThat(view.status()).isEqualTo(AccountStatus.BANNED);
		verify(sessions).endAll(user.getId());
		verify(auditLog).recordUserAction(ADMIN_ID, AuditAction.USER_LOCKED, user.getId(), Map.of("reason", REASON));
		verify(events).publishEvent(new UserBanned(user.getId(), NOW));
	}

	@Test
	void adminCannotLockTheirOwnAccount() {
		User admin = givenUser(UserFixtures.admin());

		assertThatThrownBy(() -> administration.lockUser(admin.getId(), admin.getId(), REASON))
			.isInstanceOf(OperationNotAllowedException.class)
			.extracting(UserAdministrationServiceImplTests::codeOf)
			.isEqualTo("CANNOT_LOCK_OWN_ACCOUNT");
	}

	@Test
	void anotherAdminCannotBeLocked() {
		User otherAdmin = givenUser(UserFixtures.admin());

		assertThatThrownBy(() -> administration.lockUser(ADMIN_ID, otherAdmin.getId(), REASON))
			.extracting(UserAdministrationServiceImplTests::codeOf)
			.isEqualTo("CANNOT_LOCK_ADMIN");
		verifyNoInteractions(sessions, auditLog, events);
	}

	@Test
	void lockingALockedAccountIsAConflictAndChangesNothing() {
		User user = givenUser(UserFixtures.lockedUser());

		assertThatThrownBy(() -> administration.lockUser(ADMIN_ID, user.getId(), REASON))
			.isInstanceOf(ConflictException.class);
		verifyNoInteractions(sessions, auditLog, events);
	}

	@Test
	void unlockingRestoresTheAccountAuditsAndNotifies() {
		User user = givenUser(UserFixtures.lockedUser());

		AdminUserView view = administration.unlockUser(ADMIN_ID, user.getId(), "Appeal accepted");

		assertThat(view.status()).isEqualTo(AccountStatus.ACTIVE);
		verify(auditLog).recordUserAction(ADMIN_ID, AuditAction.USER_UNLOCKED, user.getId(),
				Map.of("reason", "Appeal accepted"));
		verify(events).publishEvent(new UserUnbanned(user.getId(), NOW));
	}

	@Test
	void unlockingAnAccountThatIsNotLockedIsAConflict() {
		User user = givenUser(UserFixtures.activeUser());

		assertThatThrownBy(() -> administration.unlockUser(ADMIN_ID, user.getId(), REASON))
			.extracting(UserAdministrationServiceImplTests::codeOf)
			.isEqualTo("ACCOUNT_NOT_LOCKED");
		verifyNoInteractions(auditLog, events);
	}

	@Test
	void singleUserIsReturnedAsAnAdminView() {
		User user = givenUser(UserFixtures.pendingUser());

		AdminUserView view = administration.getUser(user.getId());

		assertThat(view.id()).isEqualTo(user.getId());
		assertThat(view.email()).isEqualTo(UserFixtures.EMAIL);
		assertThat(view.emailVerified()).isFalse();
	}

	@Test
	void searchReturnsOnePageAndACursorWhenMoreRowsExist() {
		List<User> fetched = List.of(UserFixtures.activeUser("a@example.com"), UserFixtures.activeUser("b@example.com"),
				UserFixtures.activeUser("c@example.com"));
		givenSearchReturns(fetched);

		CursorPage<AdminUserView> page = administration
			.searchUsers(new UserSearchCriteria("example", AccountStatus.ACTIVE, null, 2));

		assertThat(page.items()).extracting(AdminUserView::email).containsExactly("a@example.com", "b@example.com");
		assertThat(page.hasMore()).isTrue();
		assertThat(CursorPage.Cursor.decode(page.nextCursor()))
			.contains(new CursorPage.Cursor(fetched.get(1).getCreatedAt(), fetched.get(1).getId()));
	}

	@Test
	void lastPageHasNoCursor() {
		givenSearchReturns(List.of(UserFixtures.activeUser()));

		CursorPage<AdminUserView> page = administration.searchUsers(new UserSearchCriteria(null, null, null, null));

		assertThat(page.items()).hasSize(1);
		assertThat(page.hasMore()).isFalse();
		assertThat(page.nextCursor()).isNull();
	}

	@SuppressWarnings("unchecked")
	private void givenSearchReturns(List<User> fetched) {
		doReturn(fetched).when(users).findBy(any(Specification.class), any(Function.class));
	}

	private User givenUser(User user) {
		when(users.getRequired(user.getId())).thenReturn(user);
		return user;
	}

	private static String codeOf(Throwable ex) {
		return ((BusinessException) ex).getCode();
	}

}
