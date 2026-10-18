package vn.edu.uit.nearby.nearby_backend.identity.web;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import vn.edu.uit.nearby.nearby_backend.identity.application.profile.ProfileService;
import vn.edu.uit.nearby.nearby_backend.identity.web.dto.AvatarUploadUrlRequest;
import vn.edu.uit.nearby.nearby_backend.identity.web.dto.AvatarUploadUrlResponse;
import vn.edu.uit.nearby.nearby_backend.identity.web.dto.ConfirmAvatarRequest;
import vn.edu.uit.nearby.nearby_backend.identity.web.dto.ProfileResponse;
import vn.edu.uit.nearby.nearby_backend.identity.web.dto.UpdateProfileRequest;
import vn.edu.uit.nearby.nearby_backend.shared.constant.ApiPaths;
import vn.edu.uit.nearby.nearby_backend.shared.security.CurrentUser;

@Tag(name = "Profile", description = "The current user's profile and avatar (FR-ID-03)")
@RestController
@RequestMapping(ApiPaths.CURRENT_USER_BASE)
public class ProfileController {

	private final ProfileService profileService;

	public ProfileController(ProfileService profileService) {
		this.profileService = profileService;
	}

	@Operation(summary = "Get the current user's profile")
	@GetMapping
	public ProfileResponse getProfile() {
		return ProfileResponse.from(profileService.getProfile(CurrentUser.requireId()));
	}

	@Operation(summary = "Update display name and default area; the area is rounded before it is stored")
	@PutMapping
	public ProfileResponse updateProfile(@Valid @RequestBody UpdateProfileRequest request) {
		return ProfileResponse.from(profileService.updateProfile(CurrentUser.requireId(), request.toCommand()));
	}

	@Operation(summary = "Step 1 of an avatar change: get a presigned upload URL")
	@PostMapping("/avatar/upload-url")
	public AvatarUploadUrlResponse createAvatarUploadUrl(@Valid @RequestBody AvatarUploadUrlRequest request) {
		return AvatarUploadUrlResponse
			.from(profileService.createAvatarUpload(CurrentUser.requireId(), request.toCommand()));
	}

	@Operation(summary = "Step 2 of an avatar change: confirm the uploaded image")
	@PutMapping("/avatar")
	public ProfileResponse confirmAvatar(@Valid @RequestBody ConfirmAvatarRequest request) {
		return ProfileResponse.from(profileService.confirmAvatar(CurrentUser.requireId(), request.objectKey()));
	}

	@Operation(summary = "Remove the avatar")
	@DeleteMapping("/avatar")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void removeAvatar() {
		profileService.removeAvatar(CurrentUser.requireId());
	}

}
