package vn.edu.uit.nearby.nearby_backend.identity.infrastructure;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import vn.edu.uit.nearby.nearby_backend.identity.domain.AuditLogEntry;

public interface AuditLogRepository extends JpaRepository<AuditLogEntry, Long> {

	List<AuditLogEntry> findByTargetIdOrderByIdAsc(UUID targetId);

}
