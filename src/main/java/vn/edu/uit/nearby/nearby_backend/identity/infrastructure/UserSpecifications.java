package vn.edu.uit.nearby.nearby_backend.identity.infrastructure;

import java.time.Instant;
import java.util.Locale;
import java.util.UUID;

import org.jspecify.annotations.Nullable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

import vn.edu.uit.nearby.nearby_backend.identity.domain.AccountStatus;
import vn.edu.uit.nearby.nearby_backend.identity.domain.User;
import vn.edu.uit.nearby.nearby_backend.shared.web.CursorPage.Cursor;

public final class UserSpecifications {

	private static final String CREATED_AT = "createdAt";

	private static final String ID = "id";

	private static final char LIKE_ESCAPE = '\\';

	public static final Sort NEWEST_FIRST = Sort.by(Sort.Order.desc(CREATED_AT), Sort.Order.desc(ID));

	private UserSpecifications() {
	}

	public static Specification<User> hasStatus(@Nullable AccountStatus status) {
		return (root, query, cb) -> status == null ? cb.conjunction() : cb.equal(root.get("status"), status);
	}

	public static Specification<User> emailOrDisplayNameContains(@Nullable String text) {
		return (root, query, cb) -> {
			if (text == null || text.isBlank()) {
				return cb.conjunction();
			}
			String pattern = "%" + escapeLike(text.trim().toLowerCase(Locale.ROOT)) + "%";
			return cb.or(cb.like(cb.lower(root.get("email")), pattern, LIKE_ESCAPE),
					cb.like(cb.lower(root.get("displayName")), pattern, LIKE_ESCAPE));
		};
	}

	/**
	 * Keyset condition matching {@link #NEWEST_FIRST}: rows strictly after the cursor
	 * row.
	 */
	public static Specification<User> after(@Nullable Cursor cursor) {
		return (root, query, cb) -> {
			if (cursor == null) {
				return cb.conjunction();
			}
			Instant createdAt = cursor.createdAt();
			UUID id = cursor.id();
			return cb.or(cb.lessThan(root.get(CREATED_AT), createdAt),
					cb.and(cb.equal(root.get(CREATED_AT), createdAt), cb.lessThan(root.get(ID), id)));
		};
	}

	private static String escapeLike(String text) {
		return text.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
	}

}
