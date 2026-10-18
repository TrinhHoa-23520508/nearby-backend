package vn.edu.uit.nearby.nearby_backend.identity.domain;

import java.time.Instant;

import org.jspecify.annotations.Nullable;

public record LocationConsent(@Nullable Instant consentedAt, @Nullable String termsVersion,
		@Nullable Instant withdrawnAt) {

	public boolean isActive() {
		return consentedAt != null && withdrawnAt == null;
	}

	public boolean isActiveFor(String currentTermsVersion) {
		return isActive() && currentTermsVersion.equals(termsVersion);
	}

}
