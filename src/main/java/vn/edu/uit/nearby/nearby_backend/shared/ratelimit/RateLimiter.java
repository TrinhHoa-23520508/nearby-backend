package vn.edu.uit.nearby.nearby_backend.shared.ratelimit;

import java.time.Duration;
import java.util.List;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Component;

import vn.edu.uit.nearby.nearby_backend.shared.constant.RedisKeyPrefixes;
import vn.edu.uit.nearby.nearby_backend.shared.error.RateLimitExceededException;

@Component
public class RateLimiter {

	private static final RedisScript<Long> INCREMENT_WITH_EXPIRY = RedisScript.of("""
			local current = redis.call('INCR', KEYS[1])
			if current == 1 then
			  redis.call('PEXPIRE', KEYS[1], ARGV[1])
			end
			return current
			""", Long.class);

	private final StringRedisTemplate redis;

	public RateLimiter(StringRedisTemplate redis) {
		this.redis = redis;
	}

	public boolean tryAcquire(String key, int limit, Duration window) {
		Long callsInWindow = redis.execute(INCREMENT_WITH_EXPIRY, List.of(RedisKeyPrefixes.RATE_LIMIT + key),
				String.valueOf(window.toMillis()));
		return callsInWindow != null && callsInWindow <= limit;
	}

	public void acquireOrThrow(String key, int limit, Duration window) {
		if (!tryAcquire(key, limit, window)) {
			throw new RateLimitExceededException();
		}
	}

}
