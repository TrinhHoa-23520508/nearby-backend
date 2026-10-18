package vn.edu.uit.nearby.nearby_backend.identity.web.dto;

import org.jspecify.annotations.Nullable;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import vn.edu.uit.nearby.nearby_backend.identity.application.profile.UpdateProfileCommand;

/**
 * @param homeArea omitted or null clears the default area
 */
public record UpdateProfileRequest(@NotBlank @Size(min = 2, max = 60) String displayName,
		@Valid @Nullable HomeAreaPayload homeArea) {

	public UpdateProfileCommand toCommand() {
		return new UpdateProfileCommand(displayName, homeArea == null ? null : homeArea.toGeoPoint());
	}

}
