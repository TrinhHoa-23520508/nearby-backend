package vn.edu.uit.nearby.nearby_backend.shared.ratelimit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.util.UUID;
import java.util.stream.IntStream;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;

import vn.edu.uit.nearby.nearby_backend.shared.error.RateLimitExceededException;
import vn.edu.uit.nearby.nearby_backend.support.IntegrationTest;

@IntegrationTest
class RateLimiterIntegrationTests {

	private static final Duration WINDOW = Duration.ofMinutes(1);

	@Autowired
	RateLimiter rateLimiter;

	@Autowired
	StringRedisTemplate redis;

	private final String key = "area-verification:" + UUID.randomUUID();

	@Test
	void allowsCallsUpToTheLimitInclusive() {
		assertThat(IntStream.range(0, 3).mapToObj(i -> rateLimiter.tryAcquire(key, 3, WINDOW))).containsOnly(true);
	}

	@Test
	void rejectsTheFirstCallOverTheLimit() {
		IntStream.range(0, 3).forEach(i -> rateLimiter.tryAcquire(key, 3, WINDOW));

		assertThat(rateLimiter.tryAcquire(key, 3, WINDOW)).isFalse();
	}

	@Test
	void acquireOrThrowRaisesRateLimitExceeded() {
		rateLimiter.acquireOrThrow(key, 1, WINDOW);

		assertThatThrownBy(() -> rateLimiter.acquireOrThrow(key, 1, WINDOW))
			.isInstanceOf(RateLimitExceededException.class);
	}

	@Test
	void counterExpiresWithTheWindow() {
		rateLimiter.tryAcquire(key, 1, WINDOW);

		assertThat(redis.getExpire("rl:" + key)).isBetween(1L, WINDOW.toSeconds());
	}

	@Test
	void keysAreCountedIndependently() {
		rateLimiter.tryAcquire(key, 1, WINDOW);

		assertThat(rateLimiter.tryAcquire(key + ":other", 1, WINDOW)).isTrue();
	}

}
