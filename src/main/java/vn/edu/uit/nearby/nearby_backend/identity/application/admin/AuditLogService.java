package vn.edu.uit.nearby.nearby_backend.identity.application.admin;

import java.util.Map;
import java.util.UUID;

import vn.edu.uit.nearby.nearby_backend.identity.domain.AuditAction;

public interface AuditLogService {

	/**
	 * Appends an entry in the caller's transaction, so a failed write also rejects the
	 * admin action (FR-MD-04).
	 */
	void recordUserAction(UUID actorId, AuditAction action, UUID targetUserId, Map<String, String> detail);

}
