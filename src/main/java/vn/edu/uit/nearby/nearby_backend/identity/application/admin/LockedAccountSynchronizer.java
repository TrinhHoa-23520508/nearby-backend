package vn.edu.uit.nearby.nearby_backend.identity.application.admin;

import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

import vn.edu.uit.nearby.nearby_backend.identity.UserBanned;
import vn.edu.uit.nearby.nearby_backend.identity.UserUnbanned;
import vn.edu.uit.nearby.nearby_backend.identity.application.port.LockedAccountRegistry;

/**
 * Updates the locked-account registry only after the lock is committed, so a rolled-back
 * lock never blocks the user's tokens.
 */
@Component
public class LockedAccountSynchronizer {

	private final LockedAccountRegistry lockedAccounts;

	public LockedAccountSynchronizer(LockedAccountRegistry lockedAccounts) {
		this.lockedAccounts = lockedAccounts;
	}

	@TransactionalEventListener
	void onUserBanned(UserBanned event) {
		lockedAccounts.markLocked(event.userId());
	}

	@TransactionalEventListener
	void onUserUnbanned(UserUnbanned event) {
		lockedAccounts.clear(event.userId());
	}

}
