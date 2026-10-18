package vn.edu.uit.nearby.nearby_backend.identity.infrastructure;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import vn.edu.uit.nearby.nearby_backend.identity.domain.StoredRefreshToken;

public interface RefreshTokenRepository extends JpaRepository<StoredRefreshToken, UUID> {

	Optional<StoredRefreshToken> findByTokenHash(String tokenHash);

	/**
	 * Conditional update so two concurrent refreshes with the same token cannot both win.
	 * @return 1 when this call revoked the token, 0 when it was already revoked
	 */
	@Modifying
	@Query("update StoredRefreshToken t set t.revokedAt = :now where t.id = :id and t.revokedAt is null")
	int revokeIfActive(UUID id, Instant now);

	@Modifying
	@Query("update StoredRefreshToken t set t.revokedAt = :now where t.familyId = :familyId and t.revokedAt is null")
	int revokeFamily(UUID familyId, Instant now);

	@Modifying
	@Query("update StoredRefreshToken t set t.revokedAt = :now where t.userId = :userId and t.revokedAt is null")
	int revokeAllOfUser(UUID userId, Instant now);

	@Modifying
	@Query("delete from StoredRefreshToken t where t.expiresAt < :cutoff")
	int deleteExpiredBefore(Instant cutoff);

}
