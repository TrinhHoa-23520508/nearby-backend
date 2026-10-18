package vn.edu.uit.nearby.nearby_backend.identity.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;

import vn.edu.uit.nearby.nearby_backend.shared.error.BusinessException;
import vn.edu.uit.nearby.nearby_backend.shared.error.ConflictException;
import vn.edu.uit.nearby.nearby_backend.shared.error.OperationNotAllowedException;

class AccountStatusTests {

	@ParameterizedTest
	@CsvSource({ "PENDING,ACTIVE", "ACTIVE,ACTIVE", "BANNED,BANNED" })
	void emailVerificationActivatesOnlyPendingAccounts(AccountStatus from, AccountStatus expected) {
		assertThat(from.afterEmailVerified()).isEqualTo(expected);
	}

	@ParameterizedTest
	@EnumSource(value = AccountStatus.class, names = { "PENDING", "ACTIVE" })
	void lockingAnUnlockedAccountBansIt(AccountStatus from) {
		assertThat(from.afterLock()).isEqualTo(AccountStatus.BANNED);
	}

	@Test
	void lockingABannedAccountIsAConflict() {
		assertThatThrownBy(AccountStatus.BANNED::afterLock).isInstanceOf(ConflictException.class)
			.extracting(ex -> ((BusinessException) ex).getCode())
			.isEqualTo("ACCOUNT_ALREADY_LOCKED");
	}

	@ParameterizedTest
	@CsvSource({ "true,ACTIVE", "false,PENDING" })
	void unlockingRestoresTheStatusTheEmailVerificationAllows(boolean hasVerifiedEmail, AccountStatus expected) {
		assertThat(AccountStatus.BANNED.afterUnlock(hasVerifiedEmail)).isEqualTo(expected);
	}

	@ParameterizedTest
	@EnumSource(value = AccountStatus.class, names = { "PENDING", "ACTIVE" })
	void unlockingAnAccountThatIsNotLockedIsAConflict(AccountStatus from) {
		assertThatThrownBy(() -> from.afterUnlock(true)).isInstanceOf(ConflictException.class)
			.extracting(ex -> ((BusinessException) ex).getCode())
			.isEqualTo("ACCOUNT_NOT_LOCKED");
	}

	@Test
	void onlyActiveAccountsCanLogIn() {
		assertThatCode(AccountStatus.ACTIVE::ensureCanLogIn).doesNotThrowAnyException();
	}

	@ParameterizedTest
	@CsvSource({ "PENDING,EMAIL_NOT_VERIFIED", "BANNED,ACCOUNT_LOCKED" })
	void inactiveAccountsCannotLogIn(AccountStatus status, String expectedCode) {
		assertThatThrownBy(status::ensureCanLogIn).isInstanceOf(OperationNotAllowedException.class)
			.extracting(ex -> ((BusinessException) ex).getCode())
			.isEqualTo(expectedCode);
	}

	@Test
	void onlyBannedCountsAsLocked() {
		assertThat(AccountStatus.BANNED.isLocked()).isTrue();
		assertThat(AccountStatus.ACTIVE.isLocked()).isFalse();
		assertThat(AccountStatus.PENDING.isLocked()).isFalse();
	}

}
