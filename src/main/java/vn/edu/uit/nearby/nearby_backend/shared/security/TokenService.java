package vn.edu.uit.nearby.nearby_backend.shared.security;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;

import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Component;

import vn.edu.uit.nearby.nearby_backend.shared.constant.SecurityConstants;

@Component
public class TokenService {

	private final JwtEncoder jwtEncoder;

	private final JwtProperties properties;

	private final Clock clock;

	private final SecureRandom secureRandom = new SecureRandom();

	public TokenService(JwtEncoder jwtEncoder, JwtProperties properties, Clock clock) {
		this.jwtEncoder = jwtEncoder;
		this.properties = properties;
		this.clock = clock;
	}

	public AccessToken issueAccessToken(UUID userId, String role) {
		Instant issuedAt = clock.instant();
		Instant expiresAt = issuedAt.plus(properties.accessTokenTtl());
		JwtClaimsSet claims = JwtClaimsSet.builder()
			.issuer(properties.issuer())
			.subject(userId.toString())
			.issuedAt(issuedAt)
			.expiresAt(expiresAt)
			.id(UUID.randomUUID().toString())
			.claim(SecurityConstants.ROLES_CLAIM, List.of(role))
			.build();
		JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
		String value = jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
		return new AccessToken(value, expiresAt);
	}

	public RefreshToken issueRefreshToken() {
		byte[] bytes = new byte[SecurityConstants.REFRESH_TOKEN_BYTES];
		secureRandom.nextBytes(bytes);
		String value = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
		return new RefreshToken(value, sha256Hex(value), clock.instant().plus(properties.refreshTokenTtl()));
	}

	public static String sha256Hex(String token) {
		try {
			MessageDigest digest = MessageDigest.getInstance("SHA-256");
			return HexFormat.of().formatHex(digest.digest(token.getBytes(StandardCharsets.UTF_8)));
		}
		catch (NoSuchAlgorithmException ex) {
			throw new IllegalStateException("SHA-256 is required by every JVM", ex);
		}
	}

	public record AccessToken(String value, Instant expiresAt) {
	}

	public record RefreshToken(String value, String hash, Instant expiresAt) {
	}

}
