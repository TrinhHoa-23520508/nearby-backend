package vn.edu.uit.nearby.nearby_backend.shared.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtException;

import vn.edu.uit.nearby.nearby_backend.shared.constant.Roles;
import vn.edu.uit.nearby.nearby_backend.shared.constant.SecurityConstants;

class TokenServiceTests {

	private static final Instant NOW = Instant.parse("2026-10-18T00:00:00Z");

	private final TokenService tokenService = JwtFixture.tokenService(JwtFixture.PROPERTIES,
			Clock.fixed(NOW, ZoneOffset.UTC));

	@Test
	void accessTokenCarriesUserIdAndRoleAndExpiresAfterConfiguredTtl() {
		UUID userId = UUID.randomUUID();

		TokenService.AccessToken token = tokenService.issueAccessToken(userId, Roles.ADMIN);

		assertThat(token.expiresAt()).isEqualTo(NOW.plus(Duration.ofMinutes(15)));
		assertThat(token.value().split("\\.")).hasSize(3);
		assertThat(authoritiesOf(validToken(userId, Roles.ADMIN)))
			.contains(SecurityConstants.AUTHORITY_PREFIX + Roles.ADMIN)
			.doesNotContain(SecurityConstants.AUTHORITY_PREFIX + Roles.USER);
	}

	@Test
	void decoderRejectsTokenFromAnotherIssuer() {
		JwtProperties otherIssuer = new JwtProperties(JwtFixture.PROPERTIES.secret(), "someone-else",
				Duration.ofMinutes(15), Duration.ofDays(30));
		String token = JwtFixture.tokenService(otherIssuer, Clock.systemUTC())
			.issueAccessToken(UUID.randomUUID(), Roles.USER)
			.value();

		assertThatThrownBy(() -> JwtFixture.decoder().decode(token)).isInstanceOf(JwtException.class);
	}

	@Test
	void decoderRejectsExpiredToken() {
		Clock longAgo = Clock.fixed(NOW.minus(Duration.ofDays(1)), ZoneOffset.UTC);
		String token = JwtFixture.tokenService(JwtFixture.PROPERTIES, longAgo)
			.issueAccessToken(UUID.randomUUID(), Roles.USER)
			.value();

		assertThatThrownBy(() -> JwtFixture.decoder().decode(token)).isInstanceOf(JwtException.class);
	}

	@Test
	void refreshTokenExposesOnlyItsSha256HashForStorage() {
		TokenService.RefreshToken refreshToken = tokenService.issueRefreshToken();

		assertThat(refreshToken.hash()).hasSize(64).isEqualTo(TokenService.sha256Hex(refreshToken.value()));
		assertThat(refreshToken.value()).isNotEqualTo(refreshToken.hash());
		assertThat(refreshToken.expiresAt()).isEqualTo(NOW.plus(Duration.ofDays(30)));
	}

	@Test
	void refreshTokensAreUnique() {
		assertThat(tokenService.issueRefreshToken().value()).isNotEqualTo(tokenService.issueRefreshToken().value());
	}

	@Test
	void secretShorterThan32BytesIsRejectedAtStartup() {
		assertThatThrownBy(() -> new JwtProperties("31-bytes-secret-is-not-enough!!", "nearby", Duration.ofMinutes(1),
				Duration.ofDays(1)))
			.isInstanceOf(IllegalStateException.class);
	}

	private static Jwt validToken(UUID userId, String role) {
		String value = JwtFixture.tokenService(JwtFixture.PROPERTIES, Clock.systemUTC())
			.issueAccessToken(userId, role)
			.value();
		Jwt jwt = JwtFixture.decoder().decode(value);
		assertThat(jwt.getSubject()).isEqualTo(userId.toString());
		return jwt;
	}

	private static Iterable<String> authoritiesOf(Jwt jwt) {
		return JwtFixture.authenticationConverter()
			.convert(jwt)
			.getAuthorities()
			.stream()
			.map(GrantedAuthority::getAuthority)
			.toList();
	}

}
