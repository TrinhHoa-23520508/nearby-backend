package vn.edu.uit.nearby.nearby_backend.identity.application.admin;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import vn.edu.uit.nearby.nearby_backend.identity.UserBanned;
import vn.edu.uit.nearby.nearby_backend.identity.UserUnbanned;
import vn.edu.uit.nearby.nearby_backend.identity.application.auth.UserSessions;
import vn.edu.uit.nearby.nearby_backend.identity.domain.AuditAction;
import vn.edu.uit.nearby.nearby_backend.identity.domain.User;
import vn.edu.uit.nearby.nearby_backend.identity.infrastructure.UserRepository;
import vn.edu.uit.nearby.nearby_backend.identity.infrastructure.UserSpecifications;
import vn.edu.uit.nearby.nearby_backend.shared.constant.ErrorCodes;
import vn.edu.uit.nearby.nearby_backend.shared.error.OperationNotAllowedException;
import vn.edu.uit.nearby.nearby_backend.shared.web.CursorPage;
import vn.edu.uit.nearby.nearby_backend.shared.web.CursorPage.Cursor;

@Service
public class UserAdministrationServiceImpl implements UserAdministrationService {

	private static final String REASON = "reason";

	private final UserRepository users;

	private final UserSessions sessions;

	private final AuditLogService auditLog;

	private final ApplicationEventPublisher events;

	private final Clock clock;

	public UserAdministrationServiceImpl(UserRepository users, UserSessions sessions, AuditLogService auditLog,
			ApplicationEventPublisher events, Clock clock) {
		this.users = users;
		this.sessions = sessions;
		this.auditLog = auditLog;
		this.events = events;
		this.clock = clock;
	}

	@Override
	@Transactional(readOnly = true)
	public CursorPage<AdminUserView> searchUsers(UserSearchCriteria criteria) {
		int limit = CursorPage.clampLimit(criteria.limit());
		Cursor cursor = Cursor.decode(criteria.cursor()).orElse(null);
		Specification<User> matching = Specification.allOf(UserSpecifications.hasStatus(criteria.status()),
				UserSpecifications.emailOrDisplayNameContains(criteria.query()), UserSpecifications.after(cursor));
		List<AdminUserView> fetched = users
			.findBy(matching, query -> query.sortBy(UserSpecifications.NEWEST_FIRST).limit(limit + 1).all())
			.stream()
			.map(AdminUserView::of)
			.toList();
		return CursorPage.of(fetched, limit, view -> new Cursor(view.createdAt(), view.id()));
	}

	@Override
	@Transactional(readOnly = true)
	public AdminUserView getUser(UUID userId) {
		return AdminUserView.of(users.getRequired(userId));
	}

	@Override
	@Transactional
	public AdminUserView lockUser(UUID adminId, UUID userId, String reason) {
		User user = users.getRequired(userId);
		requireLockable(adminId, user);
		Instant now = clock.instant();
		user.lock(now);
		sessions.endAll(userId);
		auditLog.recordUserAction(adminId, AuditAction.USER_LOCKED, userId, Map.of(REASON, reason));
		events.publishEvent(new UserBanned(userId, now));
		return AdminUserView.of(user);
	}

	@Override
	@Transactional
	public AdminUserView unlockUser(UUID adminId, UUID userId, String reason) {
		User user = users.getRequired(userId);
		Instant now = clock.instant();
		user.unlock(now);
		auditLog.recordUserAction(adminId, AuditAction.USER_UNLOCKED, userId, Map.of(REASON, reason));
		events.publishEvent(new UserUnbanned(userId, now));
		return AdminUserView.of(user);
	}

	private static void requireLockable(UUID adminId, User user) {
		if (user.getId().equals(adminId)) {
			throw new OperationNotAllowedException(ErrorCodes.CANNOT_LOCK_OWN_ACCOUNT,
					"You cannot lock your own account.");
		}
		if (user.isAdmin()) {
			throw new OperationNotAllowedException(ErrorCodes.CANNOT_LOCK_ADMIN,
					"Administrator accounts cannot be locked.");
		}
	}

}
