package vn.edu.uit.nearby.nearby_backend.identity.application.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static vn.edu.uit.nearby.nearby_backend.identity.domain.UserFixtures.NOW;

import java.time.Clock;
import java.time.Duration;
import java.time.ZoneOffset;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import vn.edu.uit.nearby.nearby_backend.identity.domain.StoredRefreshToken;
import vn.edu.uit.nearby.nearby_backend.identity.domain.StoredRefreshToken.RefreshTokenFamily;
import vn.edu.uit.nearby.nearby_backend.identity.domain.User;
import vn.edu.uit.nearby.nearby_backend.identity.domain.UserFixtures;
import vn.edu.uit.nearby.nearby_backend.identity.infrastructure.RefreshTokenRepository;
import vn.edu.uit.nearby.nearby_backend.shared.security.JwtFixture;
import vn.edu.uit.nearby.nearby_backend.shared.security.TokenService;

@ExtendWith(MockitoExtension.class)
class UserSessionsTests {

	private static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);

	@Mock
	RefreshTokenRepository refreshTokens;

	private UserSessions sessions() {
		return new UserSessions(JwtFixture.tokenService(JwtFixture.PROPERTIES, CLOCK), refreshTokens, CLOCK);
	}

	@Test
	void newSessionStoresOnlyTheHashOfTheRefreshTokenInANewFamily() {
		User user = UserFixtures.activeUser();

		TokenPair tokens = sessions().start(user);

		StoredRefreshToken stored = savedToken();
		assertThat(stored.getTokenHash()).isEqualTo(TokenService.sha256Hex(tokens.refreshToken()));
		assertThat(stored.getUserId()).isEqualTo(user.getId());
		assertThat(stored.getExpiresAt()).isEqualTo(tokens.refreshTokenExpiresAt());
		assertThat(tokens.accessTokenExpiresAt()).isEqualTo(NOW.plus(Duration.ofMinutes(15)));
		assertThat(tokens.refreshTokenExpiresAt()).isEqualTo(NOW.plus(Duration.ofDays(30)));
		assertThat(tokens.accessToken().split("\\.")).hasSize(3);
	}

	@Test
	void rotationKeepsTheFamily() {
		User user = UserFixtures.activeUser();
		RefreshTokenFamily family = RefreshTokenFamily.startFor(user.getId());

		sessions().rotate(user, family);

		assertThat(savedToken().family()).isEqualTo(family);
	}

	@Test
	void endingAllSessionsRevokesEveryTokenOfTheUser() {
		UUID userId = UUID.randomUUID();

		sessions().endAll(userId);

		verify(refreshTokens).revokeAllOfUser(userId, NOW);
	}

	private StoredRefreshToken savedToken() {
		ArgumentCaptor<StoredRefreshToken> saved = ArgumentCaptor.forClass(StoredRefreshToken.class);
		verify(refreshTokens).save(saved.capture());
		return saved.getValue();
	}

}
