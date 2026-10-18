package vn.edu.uit.nearby.nearby_backend.identity.domain;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;

import org.jspecify.annotations.Nullable;
import org.locationtech.jts.geom.Point;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "users")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User {

	@Id
	private UUID id;

	@Column(nullable = false)
	private String email;

	@Column(name = "password_hash", nullable = false)
	private String passwordHash;

	@Column(name = "display_name", nullable = false)
	private String displayName;

	@Column(name = "avatar_key")
	private @Nullable String avatarKey;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private UserRole role;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private AccountStatus status;

	@Getter(AccessLevel.NONE)
	@Column(name = "home_area", columnDefinition = "geography(Point,4326)")
	private @Nullable Point homeArea;

	@Getter(AccessLevel.NONE)
	@Column(name = "consent_at")
	private @Nullable Instant consentAt;

	@Getter(AccessLevel.NONE)
	@Column(name = "consent_terms_version")
	private @Nullable String consentTermsVersion;

	@Getter(AccessLevel.NONE)
	@Column(name = "consent_withdrawn_at")
	private @Nullable Instant consentWithdrawnAt;

	@Column(name = "email_verified_at")
	private @Nullable Instant emailVerifiedAt;

	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

	@Version
	private long version;

	private User(EmailAddress email, String passwordHash, String displayName, UserRole role, Instant now) {
		this.id = UUID.randomUUID();
		this.email = email.value();
		this.passwordHash = passwordHash;
		this.displayName = displayName;
		this.role = role;
		this.status = AccountStatus.PENDING;
		this.createdAt = truncated(now);
		this.updatedAt = this.createdAt;
	}

	public static User register(EmailAddress email, String passwordHash, String displayName, Instant now) {
		return new User(email, passwordHash, displayName, UserRole.USER, now);
	}

	public static User administrator(EmailAddress email, String passwordHash, String displayName, Instant now) {
		User admin = new User(email, passwordHash, displayName, UserRole.ADMIN, now);
		admin.verifyEmail(now);
		return admin;
	}

	public EmailAddress emailAddress() {
		return EmailAddress.of(email);
	}

	public boolean isAdmin() {
		return role == UserRole.ADMIN;
	}

	public boolean hasVerifiedEmail() {
		return emailVerifiedAt != null;
	}

	public boolean isAwaitingEmailVerification() {
		return status == AccountStatus.PENDING;
	}

	public boolean isLocked() {
		return status.isLocked();
	}

	public void verifyEmail(Instant now) {
		if (emailVerifiedAt == null) {
			emailVerifiedAt = now;
		}
		status = status.afterEmailVerified();
		touch(now);
	}

	public void ensureCanLogIn() {
		status.ensureCanLogIn();
	}

	public void changePassword(String newPasswordHash, Instant now) {
		passwordHash = newPasswordHash;
		touch(now);
	}

	public void updateProfile(String newDisplayName, @Nullable HomeArea newHomeArea, Instant now) {
		displayName = newDisplayName;
		homeArea = newHomeArea == null ? null : newHomeArea.toPoint();
		touch(now);
	}

	public Optional<HomeArea> homeArea() {
		return Optional.ofNullable(homeArea).map(HomeArea::from);
	}

	/**
	 * @return the key of the avatar that was replaced, so the caller can delete it
	 */
	public Optional<String> replaceAvatar(String newAvatarKey, Instant now) {
		Optional<String> previous = Optional.ofNullable(avatarKey).filter(key -> !key.equals(newAvatarKey));
		avatarKey = newAvatarKey;
		touch(now);
		return previous;
	}

	/**
	 * @return the key of the removed avatar, so the caller can delete it
	 */
	public Optional<String> removeAvatar(Instant now) {
		Optional<String> previous = Optional.ofNullable(avatarKey);
		avatarKey = null;
		touch(now);
		return previous;
	}

	public LocationConsent locationConsent() {
		return new LocationConsent(consentAt, consentTermsVersion, consentWithdrawnAt);
	}

	public void grantLocationConsent(String termsVersion, Instant now) {
		consentAt = now;
		consentTermsVersion = termsVersion;
		consentWithdrawnAt = null;
		touch(now);
	}

	public void withdrawLocationConsent(Instant now) {
		consentWithdrawnAt = now;
		touch(now);
	}

	public void lock(Instant now) {
		status = status.afterLock();
		touch(now);
	}

	public void unlock(Instant now) {
		status = status.afterUnlock(hasVerifiedEmail());
		touch(now);
	}

	private void touch(Instant now) {
		updatedAt = now;
	}

	// Cursor pagination encodes createdAt in milliseconds; storing more precision would
	// make the cursor skip rows created within the same millisecond.
	private static Instant truncated(Instant instant) {
		return instant.truncatedTo(ChronoUnit.MILLIS);
	}

}
