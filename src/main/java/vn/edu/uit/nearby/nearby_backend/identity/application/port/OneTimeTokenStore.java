package vn.edu.uit.nearby.nearby_backend.identity.application.port;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

/**
 * Single-use tokens sent by email. Only a hash of the token is stored.
 */
public interface OneTimeTokenStore {

	/**
	 * @return the raw token to put in the email link
	 */
	String issue(OneTimeTokenPurpose purpose, UUID userId, Duration ttl);

	Optional<UUID> find(OneTimeTokenPurpose purpose, String token);

	/**
	 * Atomically reads and deletes the token, so it can be used only once.
	 */
	Optional<UUID> consume(OneTimeTokenPurpose purpose, String token);

}
