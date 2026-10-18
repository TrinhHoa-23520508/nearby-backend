package vn.edu.uit.nearby.nearby_backend.identity.domain;

import vn.edu.uit.nearby.nearby_backend.shared.constant.ErrorCodes;
import vn.edu.uit.nearby.nearby_backend.shared.error.ConflictException;
import vn.edu.uit.nearby.nearby_backend.shared.error.OperationNotAllowedException;

/**
 * Account state machine: PENDING --verify email--> ACTIVE; PENDING/ACTIVE --lock-->
 * BANNED; BANNED --unlock--> ACTIVE, or back to PENDING when the email was never
 * verified.
 */
public enum AccountStatus {

	PENDING {
		@Override
		public AccountStatus afterEmailVerified() {
			return ACTIVE;
		}

		@Override
		public void ensureCanLogIn() {
			throw new OperationNotAllowedException(ErrorCodes.EMAIL_NOT_VERIFIED,
					"Verify your email address before logging in.");
		}
	},

	ACTIVE {
		@Override
		public AccountStatus afterEmailVerified() {
			return ACTIVE;
		}

		@Override
		public void ensureCanLogIn() {
		}
	},

	BANNED {
		@Override
		public AccountStatus afterEmailVerified() {
			return BANNED;
		}

		@Override
		public void ensureCanLogIn() {
			throw new OperationNotAllowedException(ErrorCodes.ACCOUNT_LOCKED, "This account cannot log in.");
		}

		@Override
		public AccountStatus afterLock() {
			throw new ConflictException(ErrorCodes.ACCOUNT_ALREADY_LOCKED, "The account is already locked.");
		}

		@Override
		public AccountStatus afterUnlock(boolean hasVerifiedEmail) {
			return hasVerifiedEmail ? ACTIVE : PENDING;
		}
	};

	public abstract AccountStatus afterEmailVerified();

	public abstract void ensureCanLogIn();

	public AccountStatus afterLock() {
		return BANNED;
	}

	public AccountStatus afterUnlock(boolean hasVerifiedEmail) {
		throw new ConflictException(ErrorCodes.ACCOUNT_NOT_LOCKED, "The account is not locked.");
	}

	public boolean isLocked() {
		return this == BANNED;
	}

}
