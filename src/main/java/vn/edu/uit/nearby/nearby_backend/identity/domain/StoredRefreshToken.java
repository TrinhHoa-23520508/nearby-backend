package vn.edu.uit.nearby.nearby_backend.identity.domain;

import java.time.Instant;
import java.util.UUID;

import org.jspecify.annotations.Nullable;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Hash of an issued refresh token. Every rotation stays in the same family, so reusing a
 * rotated token reveals a stolen token and revokes the whole family (FR-ID-02).
 */
@Getter
@Entity
@Table(name = "refresh_tokens")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class StoredRefreshToken {

	@Id
	private UUID id;

	@Column(name = "user_id", nullable = false, updatable = false)
	private UUID userId;

	@Column(name = "token_hash", nullable = false, updatable = false)
	private String tokenHash;

	@Column(name = "family_id", nullable = false, updatable = false)
	private UUID familyId;

	@Column(name = "expires_at", nullable = false, updatable = false)
	private Instant expiresAt;

	@Column(name = "revoked_at")
	private @Nullable Instant revokedAt;

	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	private StoredRefreshToken(UUID userId, UUID familyId, String tokenHash, Instant expiresAt, Instant now) {
		this.id = UUID.randomUUID();
		this.userId = userId;
		this.familyId = familyId;
		this.tokenHash = tokenHash;
		this.expiresAt = expiresAt;
		this.createdAt = now;
	}

	public static StoredRefreshToken issue(RefreshTokenFamily family, String tokenHash, Instant expiresAt,
			Instant now) {
		return new StoredRefreshToken(family.userId(), family.familyId(), tokenHash, expiresAt, now);
	}

	public boolean isRevoked() {
		return revokedAt != null;
	}

	public boolean isExpiredAt(Instant now) {
		return !expiresAt.isAfter(now);
	}

	public RefreshTokenFamily family() {
		return new RefreshTokenFamily(userId, familyId);
	}

	public record RefreshTokenFamily(UUID userId, UUID familyId) {

		public static RefreshTokenFamily startFor(UUID userId) {
			return new RefreshTokenFamily(userId, UUID.randomUUID());
		}

	}

}
