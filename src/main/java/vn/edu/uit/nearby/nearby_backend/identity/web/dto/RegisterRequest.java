package vn.edu.uit.nearby.nearby_backend.identity.web.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(@NotBlank @Email @Size(max = 255) String email,
		@NotBlank @Size(max = 200) String password, @NotBlank @Size(min = 2, max = 60) String displayName) {

}
