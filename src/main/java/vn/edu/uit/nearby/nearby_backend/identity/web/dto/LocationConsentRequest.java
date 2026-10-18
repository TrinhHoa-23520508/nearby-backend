package vn.edu.uit.nearby.nearby_backend.identity.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LocationConsentRequest(@NotBlank @Size(max = 20) String termsVersion) {

}
