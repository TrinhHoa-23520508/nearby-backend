package vn.edu.uit.nearby.nearby_backend.identity.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import vn.edu.uit.nearby.nearby_backend.shared.error.InvalidRequestException;
import vn.edu.uit.nearby.nearby_backend.shared.geo.GeoPoint;

class ValueObjectTests {

	private static final Instant NOW = UserFixtures.NOW;

	@Test
	void emailIsTrimmedAndLowercased() {
		EmailAddress email = EmailAddress.of("  Binh.Tran@Example.COM ");

		assertThat(email.value()).isEqualTo("binh.tran@example.com");
		assertThat(email.localPart()).isEqualTo("binh.tran");
	}

	@ParameterizedTest
	@NullAndEmptySource
	@ValueSource(strings = { "no-at-sign", "two@@example.com", "spaces in@example.com", "missing@tld" })
	void malformedEmailIsRejected(String value) {
		assertThatThrownBy(() -> EmailAddress.of(value)).isInstanceOf(InvalidRequestException.class);
	}

	@Test
	void emailLongerThanTheColumnIsRejected() {
		String tooLong = "a".repeat(250) + "@example.com";

		assertThatThrownBy(() -> EmailAddress.of(tooLong)).isInstanceOf(InvalidRequestException.class);
	}

	@Test
	void homeAreaIsRoundedToThreeDecimals() {
		HomeArea area = HomeArea.roundedFrom(new GeoPoint(10.870149, 106.803151));

		assertThat(area.center()).isEqualTo(new GeoPoint(10.870, 106.803));
	}

	@Test
	void homeAreaRoundsHalfUp() {
		HomeArea area = HomeArea.roundedFrom(new GeoPoint(10.0005, -106.0005));

		assertThat(area.center()).isEqualTo(new GeoPoint(10.001, -106.001));
	}

	@Test
	void homeAreaSurvivesTheConversionToAPointAndBack() {
		HomeArea area = HomeArea.roundedFrom(new GeoPoint(10.87, 106.8));

		assertThat(HomeArea.from(area.toPoint())).isEqualTo(area);
	}

	@Test
	void consentIsActiveOnlyWhenGivenAndNotWithdrawn() {
		assertThat(new LocationConsent(null, null, null).isActive()).isFalse();
		assertThat(new LocationConsent(NOW, "1.0", null).isActive()).isTrue();
		assertThat(new LocationConsent(NOW, "1.0", NOW).isActive()).isFalse();
	}

	@Test
	void consentToAnOlderTermsVersionIsNotActiveForTheCurrentOne() {
		LocationConsent consent = new LocationConsent(NOW, "1.0", null);

		assertThat(consent.isActiveFor("1.0")).isTrue();
		assertThat(consent.isActiveFor("2.0")).isFalse();
	}

}
