package vn.edu.uit.nearby.nearby_backend.identity.application.admin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static vn.edu.uit.nearby.nearby_backend.identity.domain.UserFixtures.NOW;

import java.time.Clock;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import tools.jackson.databind.json.JsonMapper;
import vn.edu.uit.nearby.nearby_backend.identity.UserBanned;
import vn.edu.uit.nearby.nearby_backend.identity.UserUnbanned;
import vn.edu.uit.nearby.nearby_backend.identity.application.IdentityProperties;
import vn.edu.uit.nearby.nearby_backend.identity.application.IdentityProperties.BootstrapAdmin;
import vn.edu.uit.nearby.nearby_backend.identity.application.account.PasswordPolicyFixture;
import vn.edu.uit.nearby.nearby_backend.identity.application.port.LockedAccountRegistry;
import vn.edu.uit.nearby.nearby_backend.identity.domain.AuditAction;
import vn.edu.uit.nearby.nearby_backend.identity.domain.AuditLogEntry;
import vn.edu.uit.nearby.nearby_backend.identity.domain.User;
import vn.edu.uit.nearby.nearby_backend.identity.domain.UserRole;
import vn.edu.uit.nearby.nearby_backend.identity.domain.password.WeakPasswordException;
import vn.edu.uit.nearby.nearby_backend.identity.infrastructure.AuditLogRepository;
import vn.edu.uit.nearby.nearby_backend.identity.infrastructure.UserRepository;

@ExtendWith(MockitoExtension.class)
class AdminSupportTests {

	private static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);

	@Mock
	AuditLogRepository auditLogRepository;

	@Mock
	LockedAccountRegistry lockedAccounts;

	@Mock
	UserRepository users;

	@Mock
	PasswordEncoder passwordEncoder;

	@Test
	void auditEntryStoresTheDetailAsJson() {
		UUID actorId = UUID.randomUUID();
		UUID targetId = UUID.randomUUID();
		AuditLogService auditLog = new AuditLogServiceImpl(auditLogRepository, JsonMapper.builder().build(), CLOCK);

		auditLog.recordUserAction(actorId, AuditAction.USER_LOCKED, targetId, Map.of("reason", "Spam \"links\""));

		ArgumentCaptor<AuditLogEntry> saved = ArgumentCaptor.forClass(AuditLogEntry.class);
		verify(auditLogRepository).save(saved.capture());
		assertThat(saved.getValue().getDetail()).isEqualTo("{\"reason\":\"Spam \\\"links\\\"\"}");
		assertThat(saved.getValue().getActorId()).isEqualTo(actorId);
		assertThat(saved.getValue().getCreatedAt()).isEqualTo(NOW);
	}

	@Test
	void bannedAccountIsMarkedLockedAndUnbannedAccountIsCleared() {
		LockedAccountSynchronizer synchronizer = new LockedAccountSynchronizer(lockedAccounts);
		UUID userId = UUID.randomUUID();

		synchronizer.onUserBanned(new UserBanned(userId, NOW));
		synchronizer.onUserUnbanned(new UserUnbanned(userId, NOW));

		verify(lockedAccounts).markLocked(userId);
		verify(lockedAccounts).clear(userId);
	}

	@Test
	void bootstrapAdminIsCreatedWhenConfiguredAndMissing() {
		when(users.existsByEmail("root@nearby.vn")).thenReturn(false);
		when(passwordEncoder.encode("Admin12345")).thenReturn("hash");

		initializer(new BootstrapAdmin("Root@Nearby.vn", "Admin12345", "Root")).createBootstrapAdmin();

		ArgumentCaptor<User> saved = ArgumentCaptor.forClass(User.class);
		verify(users).save(saved.capture());
		assertThat(saved.getValue().getRole()).isEqualTo(UserRole.ADMIN);
		assertThat(saved.getValue().getEmail()).isEqualTo("root@nearby.vn");
		assertThat(saved.getValue().hasVerifiedEmail()).isTrue();
	}

	@Test
	void existingBootstrapAdminIsLeftAlone() {
		when(users.existsByEmail("root@nearby.vn")).thenReturn(true);

		initializer(new BootstrapAdmin("root@nearby.vn", "Admin12345", "Root")).createBootstrapAdmin();

		verify(users, never()).save(any());
	}

	@Test
	void nothingIsCreatedWithoutConfiguration() {
		initializer(new BootstrapAdmin("", "", "Root")).createBootstrapAdmin();
		initializer(null).createBootstrapAdmin();

		verify(users, never()).existsByEmail(any());
	}

	@Test
	void weakBootstrapPasswordFailsStartupLoudly() {
		when(users.existsByEmail("root@nearby.vn")).thenReturn(false);

		assertThatThrownBy(
				() -> initializer(new BootstrapAdmin("root@nearby.vn", "admin", "Root")).createBootstrapAdmin())
			.isInstanceOf(WeakPasswordException.class);
	}

	private AdminAccountInitializer initializer(BootstrapAdmin admin) {
		IdentityProperties properties = new IdentityProperties("nearby://verify-email", "nearby://reset-password",
				new IdentityProperties.Mail(false, "no-reply@nearby.local"), admin);
		return new AdminAccountInitializer(users, PasswordPolicyFixture.standard(), passwordEncoder, properties, CLOCK);
	}

}
