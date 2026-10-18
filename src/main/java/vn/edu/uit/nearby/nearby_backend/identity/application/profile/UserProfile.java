package vn.edu.uit.nearby.nearby_backend.identity.application.profile;

import java.time.Instant;
import java.util.UUID;

import org.jspecify.annotations.Nullable;

import vn.edu.uit.nearby.nearby_backend.identity.domain.AccountStatus;
import vn.edu.uit.nearby.nearby_backend.identity.domain.UserRole;
import vn.edu.uit.nearby.nearby_backend.shared.geo.GeoPoint;

public record UserProfile(UUID id, String email, String displayName, @Nullable String avatarUrl, UserRole role,
		AccountStatus status, @Nullable GeoPoint homeArea, boolean emailVerified, Instant createdAt) {

}
