package vn.edu.uit.nearby.nearby_backend.identity.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LogoutRequest(@NotBlank @Size(max = 100) String refreshToken) {

}
