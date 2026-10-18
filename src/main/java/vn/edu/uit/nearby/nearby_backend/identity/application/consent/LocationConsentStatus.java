package vn.edu.uit.nearby.nearby_backend.identity.application.consent;

import java.time.Instant;

import org.jspecify.annotations.Nullable;

/**
 * @param granted true only for an active consent to the current terms version
 */
public record LocationConsentStatus(boolean granted, @Nullable String termsVersion, @Nullable Instant consentedAt,
		@Nullable Instant withdrawnAt, String currentTermsVersion) {

}
