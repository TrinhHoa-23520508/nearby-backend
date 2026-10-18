package vn.edu.uit.nearby.nearby_backend.identity.application.consent;

import java.util.UUID;

import vn.edu.uit.nearby.nearby_backend.shared.error.InvalidRequestException;

public interface LocationConsentService {

	LocationConsentStatus getConsent(UUID userId);

	/**
	 * Records the consent time and terms version (FR-ID-06).
	 * @throws InvalidRequestException {@code OUTDATED_TERMS_VERSION} when the client did
	 * not show the current terms
	 */
	LocationConsentStatus grantConsent(UUID userId, String termsVersion);

	/**
	 * Withdraws an active consent and publishes {@code LocationConsentWithdrawn}; does
	 * nothing when there is no active consent.
	 */
	void withdrawConsent(UUID userId);

}
