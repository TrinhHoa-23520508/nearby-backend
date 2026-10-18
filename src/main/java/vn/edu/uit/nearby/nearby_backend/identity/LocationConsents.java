package vn.edu.uit.nearby.nearby_backend.identity;

import java.util.UUID;

import vn.edu.uit.nearby.nearby_backend.shared.constant.ErrorCodes;
import vn.edu.uit.nearby.nearby_backend.shared.error.OperationNotAllowedException;

/**
 * Location data processing consent (FR-ID-06). Area verification and listings with a
 * precise location must check it before collecting coordinates.
 */
public interface LocationConsents {

	/**
	 * @return true when the user consented to the current terms version and has not
	 * withdrawn; false for an unknown user
	 */
	boolean hasConsented(UUID userId);

	/**
	 * @throws OperationNotAllowedException with code {@code LOCATION_CONSENT_REQUIRED}
	 * when {@link #hasConsented(UUID)} is false
	 */
	default void requireConsent(UUID userId) {
		if (!hasConsented(userId)) {
			throw new OperationNotAllowedException(ErrorCodes.LOCATION_CONSENT_REQUIRED,
					"Consent to location data processing is required.");
		}
	}

}
