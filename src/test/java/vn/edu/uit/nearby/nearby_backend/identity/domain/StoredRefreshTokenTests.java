package vn.edu.uit.nearby.nearby_backend.identity.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import vn.edu.uit.nearby.nearby_backend.identity.domain.StoredRefreshToken.RefreshTokenFamily;

class StoredRefreshTokenTests {

	private static final Instant NOW = UserFixtures.NOW;

	private static final Instant EXPIRES_AT = NOW.plus(Duration.ofDays(30));

	private final RefreshTokenFamily family = RefreshTokenFamily.startFor(UUID.randomUUID());

	@Test
	void newTokenBelongsToItsFamilyAndIsActive() {
		StoredRefreshToken token = StoredRefreshToken.issue(family, "hash", EXPIRES_AT, NOW);

		assertThat(token.getId()).isNotNull();
		assertThat(token.family()).isEqualTo(family);
		assertThat(token.getTokenHash()).isEqualTo("hash");
		assertThat(token.getCreatedAt()).isEqualTo(NOW);
		assertThat(token.isRevoked()).isFalse();
	}

	@Test
	void tokenExpiresExactlyAtItsExpiryTime() {
		StoredRefreshToken token = StoredRefreshToken.issue(family, "hash", EXPIRES_AT, NOW);

		assertThat(token.isExpiredAt(EXPIRES_AT.minusMillis(1))).isFalse();
		assertThat(token.isExpiredAt(EXPIRES_AT)).isTrue();
	}

	@Test
	void eachStartedFamilyIsNew() {
		UUID userId = UUID.randomUUID();

		assertThat(RefreshTokenFamily.startFor(userId).familyId())
			.isNotEqualTo(RefreshTokenFamily.startFor(userId).familyId());
	}

	@Test
	void auditEntryTargetsTheUser() {
		UUID actorId = UUID.randomUUID();
		UUID targetId = UUID.randomUUID();

		AuditLogEntry entry = AuditLogEntry.onUser(actorId, AuditAction.USER_LOCKED, targetId, "{}", NOW);

		assertThat(entry.getActorId()).isEqualTo(actorId);
		assertThat(entry.getTargetId()).isEqualTo(targetId);
		assertThat(entry.getTargetType()).isEqualTo(AuditLogEntry.USER_TARGET);
		assertThat(entry.getAction()).isEqualTo(AuditAction.USER_LOCKED);
		assertThat(entry.getDetail()).isEqualTo("{}");
		assertThat(entry.getCreatedAt()).isEqualTo(NOW);
	}

}
