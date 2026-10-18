package vn.edu.uit.nearby.nearby_backend.identity.application.port;

import java.util.UUID;

/**
 * Fast lookup of locked accounts, so access tokens issued before the lock are rejected
 * immediately instead of when they expire.
 */
public interface LockedAccountRegistry {

	void markLocked(UUID userId);

	void clear(UUID userId);

	boolean isLocked(UUID userId);

}
