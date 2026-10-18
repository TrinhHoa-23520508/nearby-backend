package vn.edu.uit.nearby.nearby_backend.identity.application.admin;

import java.util.UUID;

import vn.edu.uit.nearby.nearby_backend.shared.error.ConflictException;
import vn.edu.uit.nearby.nearby_backend.shared.error.OperationNotAllowedException;
import vn.edu.uit.nearby.nearby_backend.shared.web.CursorPage;

public interface UserAdministrationService {

	/**
	 * Newest accounts first.
	 */
	CursorPage<AdminUserView> searchUsers(UserSearchCriteria criteria);

	AdminUserView getUser(UUID userId);

	/**
	 * Bans the account, revokes every session, writes the audit log and publishes
	 * {@code UserBanned} (FR-ID-05).
	 * @throws OperationNotAllowedException {@code CANNOT_LOCK_OWN_ACCOUNT} or
	 * {@code CANNOT_LOCK_ADMIN}
	 * @throws ConflictException {@code ACCOUNT_ALREADY_LOCKED}
	 */
	AdminUserView lockUser(UUID adminId, UUID userId, String reason);

	/**
	 * @throws ConflictException {@code ACCOUNT_NOT_LOCKED}
	 */
	AdminUserView unlockUser(UUID adminId, UUID userId, String reason);

}
