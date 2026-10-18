package vn.edu.uit.nearby.nearby_backend.identity.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UnlockUserRequest(@NotBlank @Size(max = 200) String reason) {

}
