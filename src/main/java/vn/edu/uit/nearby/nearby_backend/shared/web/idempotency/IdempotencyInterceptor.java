package vn.edu.uit.nearby.nearby_backend.shared.web.idempotency;

import java.time.Duration;

import org.jspecify.annotations.Nullable;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import vn.edu.uit.nearby.nearby_backend.shared.constant.ApiLimits;
import vn.edu.uit.nearby.nearby_backend.shared.constant.ErrorCodes;
import vn.edu.uit.nearby.nearby_backend.shared.constant.HeaderNames;
import vn.edu.uit.nearby.nearby_backend.shared.constant.RedisKeyPrefixes;
import vn.edu.uit.nearby.nearby_backend.shared.error.ConflictException;
import vn.edu.uit.nearby.nearby_backend.shared.error.InvalidRequestException;
import vn.edu.uit.nearby.nearby_backend.shared.security.CurrentUser;

@Component
public class IdempotencyInterceptor implements HandlerInterceptor {

	private static final String RESERVED_KEY_ATTRIBUTE = IdempotencyInterceptor.class.getName() + ".reservedKey";

	private static final String ANONYMOUS_OWNER = "anonymous";

	private final StringRedisTemplate redis;

	private final Duration ttl;

	public IdempotencyInterceptor(StringRedisTemplate redis, IdempotencyProperties properties) {
		this.redis = redis;
		this.ttl = properties.ttl();
	}

	@Override
	public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
		if (!isIdempotentEndpoint(handler)) {
			return true;
		}
		String reservedKey = redisKeyFor(request, requireValidKey(request));
		if (!Boolean.TRUE.equals(redis.opsForValue().setIfAbsent(reservedKey, "1", ttl))) {
			throw new ConflictException(ErrorCodes.DUPLICATE_REQUEST, "This request has already been processed.");
		}
		request.setAttribute(RESERVED_KEY_ATTRIBUTE, reservedKey);
		return true;
	}

	@Override
	public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler,
			@Nullable Exception ex) {
		Object reservedKey = request.getAttribute(RESERVED_KEY_ATTRIBUTE);
		boolean hasFailed = ex != null || response.getStatus() >= 500;
		if (reservedKey != null && hasFailed) {
			redis.delete(reservedKey.toString());
		}
	}

	private static boolean isIdempotentEndpoint(Object handler) {
		return handler instanceof HandlerMethod method && method.hasMethodAnnotation(Idempotent.class);
	}

	private static String requireValidKey(HttpServletRequest request) {
		String key = request.getHeader(HeaderNames.IDEMPOTENCY_KEY);
		if (key == null || key.isBlank() || key.length() > ApiLimits.IDEMPOTENCY_KEY_MAX_LENGTH) {
			throw new InvalidRequestException(ErrorCodes.IDEMPOTENCY_KEY_REQUIRED, "A " + HeaderNames.IDEMPOTENCY_KEY
					+ " header of 1-" + ApiLimits.IDEMPOTENCY_KEY_MAX_LENGTH + " characters is required.");
		}
		return key;
	}

	private static String redisKeyFor(HttpServletRequest request, String idempotencyKey) {
		String owner = CurrentUser.id().map(Object::toString).orElse(ANONYMOUS_OWNER);
		return RedisKeyPrefixes.IDEMPOTENCY + owner + ":" + request.getMethod() + ":" + request.getRequestURI() + ":"
				+ idempotencyKey;
	}

}
