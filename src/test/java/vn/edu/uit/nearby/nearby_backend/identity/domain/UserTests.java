package vn.edu.uit.nearby.nearby_backend.identity.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static vn.edu.uit.nearby.nearby_backend.identity.domain.UserFixtures.NOW;

import java.time.Duration;
import java.time.Instant;

import org.junit.jupiter.api.Test;

import vn.edu.uit.nearby.nearby_backend.shared.error.ConflictException;
import vn.edu.uit.nearby.nearby_backend.shared.error.OperationNotAllowedException;
import vn.edu.uit.nearby.nearby_backend.shared.geo.GeoPoint;

class UserTests {

	private static final Instant LATER = NOW.plus(Duration.ofHours(1));

	@Test
	void registeredUserIsPendingWithRoleUserAndNormalizedEmail() {
		User user = User.register(EmailAddress.of("  An@Example.COM "), "hash", "An", NOW);

		assertThat(user.getId()).isNotNull();
		assertThat(user.getEmail()).isEqualTo("an@example.com");
		assertThat(user.getStatus()).isEqualTo(AccountStatus.PENDING);
		assertThat(user.getRole()).isEqualTo(UserRole.USER);
		assertThat(user.hasVerifiedEmail()).isFalse();
		assertThat(user.isAwaitingEmailVerification()).isTrue();
		assertThat(user.isAdmin()).isFalse();
	}

	@Test
	void creationTimeIsTruncatedToMillisecondsForCursorPagination() {
		User user = User.register(EmailAddress.of(UserFixtures.EMAIL), "hash", "An",
				Instant.parse("2026-10-18T03:00:00.123456789Z"));

		assertThat(user.getCreatedAt()).isEqualTo(Instant.parse("2026-10-18T03:00:00.123Z"));
	}

	@Test
	void verifyingEmailActivatesTheAccountAndKeepsTheFirstVerificationTime() {
		User user = UserFixtures.pendingUser();

		user.verifyEmail(NOW);
		user.verifyEmail(LATER);

		assertThat(user.getStatus()).isEqualTo(AccountStatus.ACTIVE);
		assertThat(user.getEmailVerifiedAt()).isEqualTo(NOW);
		assertThat(user.getUpdatedAt()).isEqualTo(LATER);
	}

	@Test
	void administratorIsActiveAndVerifiedFromTheStart() {
		User admin = UserFixtures.admin();

		assertThat(admin.isAdmin()).isTrue();
		assertThat(admin.getStatus()).isEqualTo(AccountStatus.ACTIVE);
		assertThat(admin.hasVerifiedEmail()).isTrue();
	}

	@Test
	void pendingUserCannotLogIn() {
		assertThatThrownBy(UserFixtures.pendingUser()::ensureCanLogIn).isInstanceOf(OperationNotAllowedException.class);
	}

	@Test
	void activeUserCanLogIn() {
		assertThatCode(UserFixtures.activeUser()::ensureCanLogIn).doesNotThrowAnyException();
	}

	@Test
	void lockAndUnlockRoundTripReturnsToActive() {
		User user = UserFixtures.activeUser();

		user.lock(NOW);
		assertThat(user.isLocked()).isTrue();
		user.unlock(LATER);

		assertThat(user.getStatus()).isEqualTo(AccountStatus.ACTIVE);
		assertThat(user.getUpdatedAt()).isEqualTo(LATER);
	}

	@Test
	void unlockingANeverVerifiedAccountReturnsToPending() {
		User user = UserFixtures.pendingUser();
		user.lock(NOW);

		user.unlock(LATER);

		assertThat(user.getStatus()).isEqualTo(AccountStatus.PENDING);
	}

	@Test
	void lockingTwiceIsAConflict() {
		User user = UserFixtures.lockedUser();

		assertThatThrownBy(() -> user.lock(LATER)).isInstanceOf(ConflictException.class);
	}

	@Test
	void verifyingEmailOfALockedAccountKeepsItLocked() {
		User user = UserFixtures.pendingUser();
		user.lock(NOW);

		user.verifyEmail(LATER);

		assertThat(user.isLocked()).isTrue();
		assertThat(user.hasVerifiedEmail()).isTrue();
	}

	@Test
	void changePasswordReplacesTheHash() {
		User user = UserFixtures.activeUser();

		user.changePassword("new-hash", LATER);

		assertThat(user.getPasswordHash()).isEqualTo("new-hash");
		assertThat(user.getUpdatedAt()).isEqualTo(LATER);
	}

	@Test
	void updateProfileStoresDisplayNameAndHomeArea() {
		User user = UserFixtures.activeUser();
		HomeArea homeArea = HomeArea.roundedFrom(new GeoPoint(10.8701, 106.8031));

		user.updateProfile("Binh", homeArea, LATER);

		assertThat(user.getDisplayName()).isEqualTo("Binh");
		assertThat(user.homeArea()).contains(homeArea);
	}

	@Test
	void updateProfileWithoutHomeAreaClearsIt() {
		User user = UserFixtures.activeUser();
		user.updateProfile("An", HomeArea.roundedFrom(new GeoPoint(10.87, 106.8)), NOW);

		user.updateProfile("An", null, LATER);

		assertThat(user.homeArea()).isEmpty();
	}

	@Test
	void replacingAnAvatarReturnsThePreviousKeyForCleanup() {
		User user = UserFixtures.activeUser();

		assertThat(user.replaceAvatar("avatars/a.jpg", NOW)).isEmpty();
		assertThat(user.replaceAvatar("avatars/b.jpg", LATER)).contains("avatars/a.jpg");
		assertThat(user.getAvatarKey()).isEqualTo("avatars/b.jpg");
	}

	@Test
	void replacingAnAvatarWithTheSameKeyDoesNotDiscardIt() {
		User user = UserFixtures.activeUser();
		user.replaceAvatar("avatars/a.jpg", NOW);

		assertThat(user.replaceAvatar("avatars/a.jpg", LATER)).isEmpty();
	}

	@Test
	void removingTheAvatarReturnsItsKey() {
		User user = UserFixtures.activeUser();
		user.replaceAvatar("avatars/a.jpg", NOW);

		assertThat(user.removeAvatar(LATER)).contains("avatars/a.jpg");
		assertThat(user.getAvatarKey()).isNull();
		assertThat(user.removeAvatar(LATER)).isEmpty();
	}

	@Test
	void newUserHasNoLocationConsent() {
		assertThat(UserFixtures.activeUser().locationConsent().isActive()).isFalse();
	}

	@Test
	void grantingConsentRecordsTimeAndTermsVersion() {
		User user = UserFixtures.activeUser();

		user.grantLocationConsent("1.0", NOW);

		LocationConsent consent = user.locationConsent();
		assertThat(consent.consentedAt()).isEqualTo(NOW);
		assertThat(consent.termsVersion()).isEqualTo("1.0");
		assertThat(consent.isActiveFor("1.0")).isTrue();
	}

	@Test
	void withdrawingConsentKeepsTheRecordButDeactivatesIt() {
		User user = UserFixtures.activeUser();
		user.grantLocationConsent("1.0", NOW);

		user.withdrawLocationConsent(LATER);

		assertThat(user.locationConsent().withdrawnAt()).isEqualTo(LATER);
		assertThat(user.locationConsent().consentedAt()).isEqualTo(NOW);
		assertThat(user.locationConsent().isActive()).isFalse();
	}

	@Test
	void consentingAgainAfterWithdrawalReactivatesConsent() {
		User user = UserFixtures.activeUser();
		user.grantLocationConsent("1.0", NOW);
		user.withdrawLocationConsent(NOW);

		user.grantLocationConsent("1.0", LATER);

		assertThat(user.locationConsent().isActive()).isTrue();
		assertThat(user.locationConsent().withdrawnAt()).isNull();
	}

}
