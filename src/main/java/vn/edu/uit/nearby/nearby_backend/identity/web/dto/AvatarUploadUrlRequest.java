package vn.edu.uit.nearby.nearby_backend.identity.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import vn.edu.uit.nearby.nearby_backend.identity.application.profile.AvatarUploadCommand;

public record AvatarUploadUrlRequest(@NotBlank @Size(max = 50) String contentType, @Positive long sizeBytes) {

	public AvatarUploadCommand toCommand() {
		return new AvatarUploadCommand(contentType, sizeBytes);
	}

}
