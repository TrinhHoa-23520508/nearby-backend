package vn.edu.uit.nearby.nearby_backend.identity.application.consent;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static vn.edu.uit.nearby.nearby_backend.identity.domain.UserFixtures.NOW;

import java.time.Clock;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import vn.edu.uit.nearby.nearby_backend.identity.LocationConsentWithdrawn;
import vn.edu.uit.nearby.nearby_backend.identity.application.IdentityParametersFixture;
import vn.edu.uit.nearby.nearby_backend.identity.domain.User;
import vn.edu.uit.nearby.nearby_backend.identity.domain.UserFixtures;
import vn.edu.uit.nearby.nearby_backend.identity.infrastructure.UserRepository;
import vn.edu.uit.nearby.nearby_backend.shared.error.BusinessException;
import vn.edu.uit.nearby.nearby_backend.shared.error.InvalidRequestException;
import vn.edu.uit.nearby.nearby_backend.shared.error.OperationNotAllowedException;

@ExtendWith(MockitoExtension.class)
class LocationConsentServiceImplTests {

	private static final String CURRENT_TERMS = "1.0";

	@Mock
	UserRepository users;

	@Mock
	ApplicationEventPublisher events;

	private final User user = UserFixtures.activeUser();

	private LocationConsentServiceImpl consentService;

	@BeforeEach
	void createService() {
		consentService = new LocationConsentServiceImpl(users, IdentityParametersFixture.seeded(), events,
				Clock.fixed(NOW, ZoneOffset.UTC));
	}

	@Test
	void consentToTheCurrentTermsIsRecordedWithItsTime() {
		when(users.getRequired(user.getId())).thenReturn(user);

		LocationConsentStatus status = consentService.grantConsent(user.getId(), CURRENT_TERMS);

		assertThat(status.granted()).isTrue();
		assertThat(status.consentedAt()).isEqualTo(NOW);
		assertThat(status.termsVersion()).isEqualTo(CURRENT_TERMS);
		assertThat(status.currentTermsVersion()).isEqualTo(CURRENT_TERMS);
	}

	@Test
	void consentToAnOutdatedTermsVersionIsRejected() {
		assertThatThrownBy(() -> consentService.grantConsent(user.getId(), "0.9"))
			.isInstanceOf(InvalidRequestException.class)
			.extracting(ex -> ((BusinessException) ex).getCode())
			.isEqualTo("OUTDATED_TERMS_VERSION");
	}

	@Test
	void statusOfAUserWhoNeverConsentedIsNotGranted() {
		when(users.getRequired(user.getId())).thenReturn(user);

		LocationConsentStatus status = consentService.getConsent(user.getId());

		assertThat(status.granted()).isFalse();
		assertThat(status.consentedAt()).isNull();
	}

	@Test
	void facadeReportsConsentOnlyForTheCurrentTermsVersion() {
		User consentedToOldTerms = UserFixtures.activeUser("old@example.com");
		consentedToOldTerms.grantLocationConsent("0.9", NOW);
		user.grantLocationConsent(CURRENT_TERMS, NOW);
		when(users.findById(user.getId())).thenReturn(Optional.of(user));
		when(users.findById(consentedToOldTerms.getId())).thenReturn(Optional.of(consentedToOldTerms));

		assertThat(consentService.hasConsented(user.getId())).isTrue();
		assertThat(consentService.hasConsented(consentedToOldTerms.getId())).isFalse();
	}

	@Test
	void unknownUserHasNotConsented() {
		UUID unknown = UUID.randomUUID();
		when(users.findById(unknown)).thenReturn(Optional.empty());

		assertThat(consentService.hasConsented(unknown)).isFalse();
	}

	@Test
	void requireConsentBlocksUsersWithoutConsent() {
		when(users.findById(user.getId())).thenReturn(Optional.of(user));

		assertThatThrownBy(() -> consentService.requireConsent(user.getId()))
			.isInstanceOf(OperationNotAllowedException.class)
			.extracting(ex -> ((BusinessException) ex).getCode())
			.isEqualTo("LOCATION_CONSENT_REQUIRED");
	}

	@Test
	void requireConsentLetsConsentedUsersThrough() {
		user.grantLocationConsent(CURRENT_TERMS, NOW);
		when(users.findById(user.getId())).thenReturn(Optional.of(user));

		assertThatCode(() -> consentService.requireConsent(user.getId())).doesNotThrowAnyException();
	}

	@Test
	void withdrawalStopsConsentAndNotifiesOtherModules() {
		user.grantLocationConsent(CURRENT_TERMS, NOW.minusSeconds(60));
		when(users.getRequired(user.getId())).thenReturn(user);

		consentService.withdrawConsent(user.getId());

		assertThat(user.locationConsent().isActive()).isFalse();
		verify(events).publishEvent(new LocationConsentWithdrawn(user.getId(), NOW));
	}

	@Test
	void withdrawingWithoutAnActiveConsentDoesNothing() {
		when(users.getRequired(user.getId())).thenReturn(user);

		consentService.withdrawConsent(user.getId());

		assertThat(user.locationConsent().withdrawnAt()).isNull();
		verifyNoInteractions(events);
		verify(users, never()).save(user);
	}

}
