package vn.edu.uit.nearby.nearby_backend.identity.application.consent;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import vn.edu.uit.nearby.nearby_backend.identity.LocationConsentWithdrawn;
import vn.edu.uit.nearby.nearby_backend.identity.LocationConsents;
import vn.edu.uit.nearby.nearby_backend.identity.application.IdentityParameters;
import vn.edu.uit.nearby.nearby_backend.identity.domain.LocationConsent;
import vn.edu.uit.nearby.nearby_backend.identity.domain.User;
import vn.edu.uit.nearby.nearby_backend.identity.infrastructure.UserRepository;
import vn.edu.uit.nearby.nearby_backend.shared.constant.ErrorCodes;
import vn.edu.uit.nearby.nearby_backend.shared.error.InvalidRequestException;

@Service
public class LocationConsentServiceImpl implements LocationConsentService, LocationConsents {

	private final UserRepository users;

	private final IdentityParameters parameters;

	private final ApplicationEventPublisher events;

	private final Clock clock;

	public LocationConsentServiceImpl(UserRepository users, IdentityParameters parameters,
			ApplicationEventPublisher events, Clock clock) {
		this.users = users;
		this.parameters = parameters;
		this.events = events;
		this.clock = clock;
	}

	@Override
	@Transactional(readOnly = true)
	public boolean hasConsented(UUID userId) {
		String currentTermsVersion = parameters.locationTermsVersion();
		return users.findById(userId)
			.map(user -> user.locationConsent().isActiveFor(currentTermsVersion))
			.orElse(false);
	}

	@Override
	@Transactional(readOnly = true)
	public LocationConsentStatus getConsent(UUID userId) {
		return statusOf(users.getRequired(userId).locationConsent(), parameters.locationTermsVersion());
	}

	@Override
	@Transactional
	public LocationConsentStatus grantConsent(UUID userId, String termsVersion) {
		String currentTermsVersion = parameters.locationTermsVersion();
		if (!currentTermsVersion.equals(termsVersion)) {
			throw new InvalidRequestException(ErrorCodes.OUTDATED_TERMS_VERSION,
					"Consent must be given to the current terms version " + currentTermsVersion + ".");
		}
		User user = users.getRequired(userId);
		user.grantLocationConsent(termsVersion, clock.instant());
		return statusOf(user.locationConsent(), currentTermsVersion);
	}

	@Override
	@Transactional
	public void withdrawConsent(UUID userId) {
		User user = users.getRequired(userId);
		if (!user.locationConsent().isActive()) {
			return;
		}
		Instant now = clock.instant();
		user.withdrawLocationConsent(now);
		events.publishEvent(new LocationConsentWithdrawn(userId, now));
	}

	private static LocationConsentStatus statusOf(LocationConsent consent, String currentTermsVersion) {
		return new LocationConsentStatus(consent.isActiveFor(currentTermsVersion), consent.termsVersion(),
				consent.consentedAt(), consent.withdrawnAt(), currentTermsVersion);
	}

}
