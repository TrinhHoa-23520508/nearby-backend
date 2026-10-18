package vn.edu.uit.nearby.nearby_backend.identity;

import java.time.Instant;
import java.util.UUID;

/**
 * An admin unlocked the account; listings hidden by {@link UserBanned} may be shown
 * again.
 */
public record UserUnbanned(UUID userId, Instant unlockedAt) {

}
