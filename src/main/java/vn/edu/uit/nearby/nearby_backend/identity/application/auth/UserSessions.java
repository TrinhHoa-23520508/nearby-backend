package vn.edu.uit.nearby.nearby_backend.identity.application.auth;

import java.time.Clock;
import java.util.UUID;

import org.springframework.stereotype.Component;

import vn.edu.uit.nearby.nearby_backend.identity.domain.StoredRefreshToken;
import vn.edu.uit.nearby.nearby_backend.identity.domain.StoredRefreshToken.RefreshTokenFamily;
import vn.edu.uit.nearby.nearby_backend.identity.domain.User;
import vn.edu.uit.nearby.nearby_backend.identity.infrastructure.RefreshTokenRepository;
import vn.edu.uit.nearby.nearby_backend.shared.security.TokenService;
import vn.edu.uit.nearby.nearby_backend.shared.security.TokenService.AccessToken;
import vn.edu.uit.nearby.nearby_backend.shared.security.TokenService.RefreshToken;

/**
 * Issues token pairs and keeps their refresh tokens; the single place that writes
 * {@code refresh_tokens}.
 */
@Component
public class UserSessions {

	private final TokenService tokenService;

	private final RefreshTokenRepository refreshTokens;

	private final Clock clock;

	public UserSessions(TokenService tokenService, RefreshTokenRepository refreshTokens, Clock clock) {
		this.tokenService = tokenService;
		this.refreshTokens = refreshTokens;
		this.clock = clock;
	}

	public TokenPair start(User user) {
		return issue(user, RefreshTokenFamily.startFor(user.getId()));
	}

	public TokenPair rotate(User user, RefreshTokenFamily family) {
		return issue(user, family);
	}

	public void endAll(UUID userId) {
		refreshTokens.revokeAllOfUser(userId, clock.instant());
	}

	private TokenPair issue(User user, RefreshTokenFamily family) {
		AccessToken accessToken = tokenService.issueAccessToken(user.getId(), user.getRole().name());
		RefreshToken refreshToken = tokenService.issueRefreshToken();
		refreshTokens
			.save(StoredRefreshToken.issue(family, refreshToken.hash(), refreshToken.expiresAt(), clock.instant()));
		return new TokenPair(accessToken.value(), accessToken.expiresAt(), refreshToken.value(),
				refreshToken.expiresAt());
	}

}
