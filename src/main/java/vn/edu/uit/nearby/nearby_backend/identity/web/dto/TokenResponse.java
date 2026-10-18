package vn.edu.uit.nearby.nearby_backend.identity.web.dto;

import java.time.Instant;

import vn.edu.uit.nearby.nearby_backend.identity.application.auth.TokenPair;
import vn.edu.uit.nearby.nearby_backend.shared.constant.SecurityConstants;

public record TokenResponse(String tokenType, String accessToken, Instant accessTokenExpiresAt, String refreshToken,
		Instant refreshTokenExpiresAt) {

	public static TokenResponse from(TokenPair tokens) {
		return new TokenResponse(SecurityConstants.BEARER_SCHEME, tokens.accessToken(), tokens.accessTokenExpiresAt(),
				tokens.refreshToken(), tokens.refreshTokenExpiresAt());
	}

}
