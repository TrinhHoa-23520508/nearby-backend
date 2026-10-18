package vn.edu.uit.nearby.nearby_backend.identity.web;

import java.util.UUID;

import org.jspecify.annotations.Nullable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import vn.edu.uit.nearby.nearby_backend.identity.application.admin.UserAdministrationService;
import vn.edu.uit.nearby.nearby_backend.identity.application.admin.UserSearchCriteria;
import vn.edu.uit.nearby.nearby_backend.identity.domain.AccountStatus;
import vn.edu.uit.nearby.nearby_backend.identity.web.dto.AdminUserResponse;
import vn.edu.uit.nearby.nearby_backend.identity.web.dto.LockUserRequest;
import vn.edu.uit.nearby.nearby_backend.identity.web.dto.UnlockUserRequest;
import vn.edu.uit.nearby.nearby_backend.shared.constant.ApiPaths;
import vn.edu.uit.nearby.nearby_backend.shared.security.CurrentUser;
import vn.edu.uit.nearby.nearby_backend.shared.web.CursorPage;

@Tag(name = "Admin: users", description = "Find, lock and unlock accounts (FR-ID-04, FR-ID-05, UC20)")
@RestController
@RequestMapping(ApiPaths.ADMIN_USERS_BASE)
@PreAuthorize("hasRole('ADMIN')")
public class AdminUserController {

	private final UserAdministrationService administrationService;

	public AdminUserController(UserAdministrationService administrationService) {
		this.administrationService = administrationService;
	}

	@Operation(summary = "Search accounts by email or display name, newest first")
	@GetMapping
	public CursorPage<AdminUserResponse> searchUsers(@RequestParam(required = false) @Nullable String query,
			@RequestParam(required = false) @Nullable AccountStatus status,
			@RequestParam(required = false) @Nullable String cursor,
			@RequestParam(required = false) @Nullable Integer limit) {
		return administrationService.searchUsers(new UserSearchCriteria(query, status, cursor, limit))
			.map(AdminUserResponse::from);
	}

	@Operation(summary = "Get one account")
	@GetMapping("/{userId}")
	public AdminUserResponse getUser(@PathVariable UUID userId) {
		return AdminUserResponse.from(administrationService.getUser(userId));
	}

	@Operation(summary = "Lock an account: revoke its sessions and hide its listings")
	@PostMapping("/{userId}/lock")
	public AdminUserResponse lockUser(@PathVariable UUID userId, @Valid @RequestBody LockUserRequest request) {
		return AdminUserResponse
			.from(administrationService.lockUser(CurrentUser.requireId(), userId, request.reason()));
	}

	@Operation(summary = "Unlock an account")
	@PostMapping("/{userId}/unlock")
	public AdminUserResponse unlockUser(@PathVariable UUID userId, @Valid @RequestBody UnlockUserRequest request) {
		return AdminUserResponse
			.from(administrationService.unlockUser(CurrentUser.requireId(), userId, request.reason()));
	}

}
