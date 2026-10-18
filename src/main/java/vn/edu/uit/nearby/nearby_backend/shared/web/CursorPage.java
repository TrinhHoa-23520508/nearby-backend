package vn.edu.uit.nearby.nearby_backend.shared.web;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;

import org.jspecify.annotations.Nullable;

import vn.edu.uit.nearby.nearby_backend.shared.constant.ApiLimits;
import vn.edu.uit.nearby.nearby_backend.shared.constant.ErrorCodes;
import vn.edu.uit.nearby.nearby_backend.shared.error.InvalidRequestException;

public record CursorPage<T>(List<T> items, @Nullable String nextCursor, boolean hasMore) {

	/**
	 * @param fetched rows queried with {@code limit + 1} so the extra row reveals whether
	 * a next page exists
	 */
	public static <T> CursorPage<T> of(List<T> fetched, int limit, Function<T, Cursor> cursorOf) {
		boolean hasMore = fetched.size() > limit;
		List<T> items = hasMore ? fetched.subList(0, limit) : fetched;
		String nextCursor = hasMore ? cursorOf.apply(items.getLast()).encode() : null;
		return new CursorPage<>(List.copyOf(items), nextCursor, hasMore);
	}

	public static int clampLimit(@Nullable Integer requestedLimit) {
		if (requestedLimit == null || requestedLimit <= 0) {
			return ApiLimits.DEFAULT_PAGE_SIZE;
		}
		return Math.min(requestedLimit, ApiLimits.MAX_PAGE_SIZE);
	}

	public record Cursor(Instant createdAt, UUID id) {

		private static final String SEPARATOR = "|";

		public String encode() {
			String raw = createdAt.toEpochMilli() + SEPARATOR + id;
			return Base64.getUrlEncoder().withoutPadding().encodeToString(raw.getBytes(StandardCharsets.UTF_8));
		}

		public static Optional<Cursor> decode(@Nullable String token) {
			if (token == null || token.isBlank()) {
				return Optional.empty();
			}
			try {
				String raw = new String(Base64.getUrlDecoder().decode(token), StandardCharsets.UTF_8);
				int separatorIndex = raw.indexOf(SEPARATOR);
				Instant createdAt = Instant.ofEpochMilli(Long.parseLong(raw.substring(0, separatorIndex)));
				return Optional.of(new Cursor(createdAt, UUID.fromString(raw.substring(separatorIndex + 1))));
			}
			catch (IllegalArgumentException | IndexOutOfBoundsException ex) {
				throw new InvalidRequestException(ErrorCodes.INVALID_CURSOR, "The pagination cursor is invalid.");
			}
		}

	}

}
