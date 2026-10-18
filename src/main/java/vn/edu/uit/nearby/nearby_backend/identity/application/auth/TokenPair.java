package vn.edu.uit.nearby.nearby_backend.identity.application.auth;

import java.time.Instant;

public record TokenPair(String accessToken, Instant accessTokenExpiresAt, String refreshToken,
		Instant refreshTokenExpiresAt) {

}
