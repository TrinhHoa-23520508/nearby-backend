package vn.edu.uit.nearby.nearby_backend.identity.application.auth;

import static org.mockito.Mockito.verify;
import static vn.edu.uit.nearby.nearby_backend.identity.domain.UserFixtures.NOW;

import java.time.Clock;
import java.time.ZoneOffset;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import vn.edu.uit.nearby.nearby_backend.identity.infrastructure.RefreshTokenRepository;

@ExtendWith(MockitoExtension.class)
class RefreshTokenCleanupJobTests {

	@Mock
	RefreshTokenRepository refreshTokens;

	@Test
	void deletesTokensThatExpiredBeforeNow() {
		new RefreshTokenCleanupJob(refreshTokens, Clock.fixed(NOW, ZoneOffset.UTC)).deleteExpiredTokens();

		verify(refreshTokens).deleteExpiredBefore(NOW);
	}

}
