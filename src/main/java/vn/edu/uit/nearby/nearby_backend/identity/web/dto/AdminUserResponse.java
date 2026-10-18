package vn.edu.uit.nearby.nearby_backend.identity.web.dto;

import java.time.Instant;
import java.util.UUID;

import vn.edu.uit.nearby.nearby_backend.identity.application.admin.AdminUserView;
import vn.edu.uit.nearby.nearby_backend.identity.domain.AccountStatus;
import vn.edu.uit.nearby.nearby_backend.identity.domain.UserRole;

public record AdminUserResponse(UUID id, String email, String displayName, UserRole role, AccountStatus status,
		boolean emailVerified, Instant createdAt, Instant updatedAt) {

	public static AdminUserResponse from(AdminUserView user) {
		return new AdminUserResponse(user.id(), user.email(), user.displayName(), user.role(), user.status(),
				user.emailVerified(), user.createdAt(), user.updatedAt());
	}

}
