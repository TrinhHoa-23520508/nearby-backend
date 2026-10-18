package vn.edu.uit.nearby.nearby_backend.identity.domain;

import java.time.Instant;
import java.util.UUID;

import org.hibernate.annotations.ColumnTransformer;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Append-only record of an admin action (FR-MD-04). Every column is non-updatable.
 */
@Getter
@Entity
@Table(name = "audit_log")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AuditLogEntry {

	public static final String USER_TARGET = "USER";

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "actor_id", updatable = false)
	private UUID actorId;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, updatable = false)
	private AuditAction action;

	@Column(name = "target_type", updatable = false)
	private String targetType;

	@Column(name = "target_id", updatable = false)
	private UUID targetId;

	@ColumnTransformer(write = "?::jsonb")
	@Column(columnDefinition = "jsonb", updatable = false)
	private String detail;

	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	private AuditLogEntry(UUID actorId, AuditAction action, UUID targetUserId, String detailJson, Instant now) {
		this.actorId = actorId;
		this.action = action;
		this.targetType = USER_TARGET;
		this.targetId = targetUserId;
		this.detail = detailJson;
		this.createdAt = now;
	}

	public static AuditLogEntry onUser(UUID actorId, AuditAction action, UUID targetUserId, String detailJson,
			Instant now) {
		return new AuditLogEntry(actorId, action, targetUserId, detailJson, now);
	}

}
