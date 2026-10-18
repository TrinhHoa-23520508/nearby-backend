package vn.edu.uit.nearby.nearby_backend.identity.application.admin;

import org.jspecify.annotations.Nullable;

import vn.edu.uit.nearby.nearby_backend.identity.domain.AccountStatus;

/**
 * @param query matched against email and display name, case-insensitively
 */
public record UserSearchCriteria(@Nullable String query, @Nullable AccountStatus status, @Nullable String cursor,
		@Nullable Integer limit) {

}
