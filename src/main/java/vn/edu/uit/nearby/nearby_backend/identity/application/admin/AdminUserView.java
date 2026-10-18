package vn.edu.uit.nearby.nearby_backend.identity.application.admin;

import java.time.Instant;
import java.util.UUID;

import vn.edu.uit.nearby.nearby_backend.identity.domain.AccountStatus;
import vn.edu.uit.nearby.nearby_backend.identity.domain.User;
import vn.edu.uit.nearby.nearby_backend.identity.domain.UserRole;

public record AdminUserView(UUID id, String email, String displayName, UserRole role, AccountStatus status,
		boolean emailVerified, Instant createdAt, Instant updatedAt) {

	static AdminUserView of(User user) {
		return new AdminUserView(user.getId(), user.getEmail(), user.getDisplayName(), user.getRole(), user.getStatus(),
				user.hasVerifiedEmail(), user.getCreatedAt(), user.getUpdatedAt());
	}

}
