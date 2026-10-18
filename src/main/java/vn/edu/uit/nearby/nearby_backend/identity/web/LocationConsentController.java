package vn.edu.uit.nearby.nearby_backend.identity.web;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import vn.edu.uit.nearby.nearby_backend.identity.application.consent.LocationConsentService;
import vn.edu.uit.nearby.nearby_backend.identity.web.dto.LocationConsentRequest;
import vn.edu.uit.nearby.nearby_backend.identity.web.dto.LocationConsentResponse;
import vn.edu.uit.nearby.nearby_backend.shared.constant.ApiPaths;
import vn.edu.uit.nearby.nearby_backend.shared.security.CurrentUser;

@Tag(name = "Location consent", description = "Consent to location data processing (FR-ID-06)")
@RestController
@RequestMapping(ApiPaths.CURRENT_USER_BASE + "/location-consent")
public class LocationConsentController {

	private final LocationConsentService consentService;

	public LocationConsentController(LocationConsentService consentService) {
		this.consentService = consentService;
	}

	@Operation(summary = "Get the current consent and the terms version clients must show")
	@GetMapping
	public LocationConsentResponse getConsent() {
		return LocationConsentResponse.from(consentService.getConsent(CurrentUser.requireId()));
	}

	@Operation(summary = "Give consent to the current terms version")
	@PutMapping
	public LocationConsentResponse grantConsent(@Valid @RequestBody LocationConsentRequest request) {
		return LocationConsentResponse
			.from(consentService.grantConsent(CurrentUser.requireId(), request.termsVersion()));
	}

	@Operation(summary = "Withdraw consent; precise locations are no longer collected")
	@DeleteMapping
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void withdrawConsent() {
		consentService.withdrawConsent(CurrentUser.requireId());
	}

}
