package vn.edu.uit.nearby.nearby_backend.identity.infrastructure;

import java.util.UUID;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import vn.edu.uit.nearby.nearby_backend.identity.application.port.LockedAccountRegistry;
import vn.edu.uit.nearby.nearby_backend.shared.constant.RedisKeyPrefixes;
import vn.edu.uit.nearby.nearby_backend.shared.security.JwtProperties;

/**
 * An entry lives as long as an access token, because older tokens have expired by then
 * and a locked account cannot obtain new ones.
 */
@Component
public class RedisLockedAccountRegistry implements LockedAccountRegistry {

	private static final String LOCKED = "1";

	private final StringRedisTemplate redis;

	private final JwtProperties jwtProperties;

	public RedisLockedAccountRegistry(StringRedisTemplate redis, JwtProperties jwtProperties) {
		this.redis = redis;
		this.jwtProperties = jwtProperties;
	}

	@Override
	public void markLocked(UUID userId) {
		redis.opsForValue().set(keyOf(userId), LOCKED, jwtProperties.accessTokenTtl());
	}

	@Override
	public void clear(UUID userId) {
		redis.delete(keyOf(userId));
	}

	@Override
	public boolean isLocked(UUID userId) {
		return Boolean.TRUE.equals(redis.hasKey(keyOf(userId)));
	}

	private static String keyOf(UUID userId) {
		return RedisKeyPrefixes.LOCKED_ACCOUNT + userId;
	}

}
