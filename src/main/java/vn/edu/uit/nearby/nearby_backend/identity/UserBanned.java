package vn.edu.uit.nearby.nearby_backend.identity;

import java.time.Instant;
import java.util.UUID;

/**
 * An admin locked the account (FR-ID-05). Listing hides the user's listings; Transaction
 * cancels the user's open transactions without penalizing the other party.
 */
public record UserBanned(UUID userId, Instant lockedAt) {

}
