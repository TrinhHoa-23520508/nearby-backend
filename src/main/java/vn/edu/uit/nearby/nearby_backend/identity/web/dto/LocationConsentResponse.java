package vn.edu.uit.nearby.nearby_backend.identity.web.dto;

import java.time.Instant;

import org.jspecify.annotations.Nullable;

import vn.edu.uit.nearby.nearby_backend.identity.application.consent.LocationConsentStatus;

public record LocationConsentResponse(boolean granted, @Nullable String termsVersion, @Nullable Instant consentedAt,
		@Nullable Instant withdrawnAt, String currentTermsVersion) {

	public static LocationConsentResponse from(LocationConsentStatus status) {
		return new LocationConsentResponse(status.granted(), status.termsVersion(), status.consentedAt(),
				status.withdrawnAt(), status.currentTermsVersion());
	}

}
