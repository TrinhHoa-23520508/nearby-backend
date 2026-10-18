package vn.edu.uit.nearby.nearby_backend.shared.ratelimit;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.TimeUnit;

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
		Long callsInWindow = redis.execute(INCREMENT_WITH_EXPIRY, List.of(redisKey(key)),
				String.valueOf(window.toMillis()));
		return callsInWindow != null && callsInWindow <= limit;
	}

	public void acquireOrThrow(String key, int limit, Duration window) {
		if (!tryAcquire(key, limit, window)) {
			throw new RateLimitExceededException(remainingWindow(key));
		}
	}

	public boolean isExhausted(String key, int limit) {
		String callsInWindow = redis.opsForValue().get(redisKey(key));
		return callsInWindow != null && Long.parseLong(callsInWindow) >= limit;
	}

	public Duration remainingWindow(String key) {
		Long remainingMillis = redis.getExpire(redisKey(key), TimeUnit.MILLISECONDS);
		return remainingMillis == null || remainingMillis < 0 ? Duration.ZERO : Duration.ofMillis(remainingMillis);
	}

	public void reset(String key) {
		redis.delete(redisKey(key));
	}

	private static String redisKey(String key) {
		return RedisKeyPrefixes.RATE_LIMIT + key;
	}

}
