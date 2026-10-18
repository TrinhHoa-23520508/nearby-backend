package vn.edu.uit.nearby.nearby_backend.identity.infrastructure;

import java.security.SecureRandom;
import java.time.Duration;
import java.util.Base64;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import vn.edu.uit.nearby.nearby_backend.identity.application.port.OneTimeTokenPurpose;
import vn.edu.uit.nearby.nearby_backend.identity.application.port.OneTimeTokenStore;
import vn.edu.uit.nearby.nearby_backend.shared.constant.RedisKeyPrefixes;
import vn.edu.uit.nearby.nearby_backend.shared.constant.SecurityConstants;
import vn.edu.uit.nearby.nearby_backend.shared.security.TokenService;

@Component
public class RedisOneTimeTokenStore implements OneTimeTokenStore {

	private final StringRedisTemplate redis;

	private final SecureRandom secureRandom = new SecureRandom();

	public RedisOneTimeTokenStore(StringRedisTemplate redis) {
		this.redis = redis;
	}

	@Override
	public String issue(OneTimeTokenPurpose purpose, UUID userId, Duration ttl) {
		String token = randomToken();
		redis.opsForValue().set(keyOf(purpose, token), userId.toString(), ttl);
		return token;
	}

	@Override
	public Optional<UUID> find(OneTimeTokenPurpose purpose, String token) {
		return Optional.ofNullable(redis.opsForValue().get(keyOf(purpose, token))).map(UUID::fromString);
	}

	@Override
	public Optional<UUID> consume(OneTimeTokenPurpose purpose, String token) {
		return Optional.ofNullable(redis.opsForValue().getAndDelete(keyOf(purpose, token))).map(UUID::fromString);
	}

	private String randomToken() {
		byte[] bytes = new byte[SecurityConstants.REFRESH_TOKEN_BYTES];
		secureRandom.nextBytes(bytes);
		return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
	}

	private static String keyOf(OneTimeTokenPurpose purpose, String token) {
		String prefix = switch (purpose) {
			case EMAIL_VERIFICATION -> RedisKeyPrefixes.EMAIL_VERIFICATION_TOKEN;
			case PASSWORD_RESET -> RedisKeyPrefixes.PASSWORD_RESET_TOKEN;
		};
		return prefix + TokenService.sha256Hex(token);
	}

}
