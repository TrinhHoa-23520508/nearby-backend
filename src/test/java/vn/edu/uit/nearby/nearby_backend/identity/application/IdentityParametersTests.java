package vn.edu.uit.nearby.nearby_backend.identity.application;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;

import org.junit.jupiter.api.Test;

class IdentityParametersTests {

	private final IdentityParameters parameters = IdentityParametersFixture.seeded();

	@Test
	void loginLockoutAllowsFiveFailuresInFifteenMinutes() {
		assertThat(parameters.loginMaxFailedAttempts()).isEqualTo(5);
		assertThat(parameters.loginLockoutWindow()).isEqualTo(Duration.ofMinutes(15));
	}

	@Test
	void emailTokenLifetimesAreConvertedToDurations() {
		assertThat(parameters.emailVerificationTtl()).isEqualTo(Duration.ofHours(24));
		assertThat(parameters.passwordResetTtl()).isEqualTo(Duration.ofMinutes(30));
	}

	@Test
	void quotasAndLimitsAreReadAsNumbers() {
		assertThat(parameters.registrationsPerHourPerIp()).isEqualTo(10);
		assertThat(parameters.accountEmailsPerHour()).isEqualTo(5);
		assertThat(parameters.imageMaxBytes()).isEqualTo(5L * 1024 * 1024);
	}

	@Test
	void termsVersionIsReadAsText() {
		assertThat(parameters.locationTermsVersion()).isEqualTo("1.0");
	}

	@Test
	void bootstrapAdminNeedsBothEmailAndPassword() {
		assertThat(new IdentityProperties.BootstrapAdmin("a@b.vn", "Secret123", "Admin").isConfigured()).isTrue();
		assertThat(new IdentityProperties.BootstrapAdmin("a@b.vn", "", "Admin").isConfigured()).isFalse();
		assertThat(new IdentityProperties.BootstrapAdmin(" ", "Secret123", "Admin").isConfigured()).isFalse();
		assertThat(new IdentityProperties.BootstrapAdmin(null, null, "Admin").isConfigured()).isFalse();
	}

}
