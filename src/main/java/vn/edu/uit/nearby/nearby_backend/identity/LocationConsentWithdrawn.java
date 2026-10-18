package vn.edu.uit.nearby.nearby_backend.identity;

import java.time.Instant;
import java.util.UUID;

/**
 * The user withdrew location consent: stop collecting precise locations. Data already
 * collected is kept and disposed of per the retention rules (SRS section 4.4).
 */
public record LocationConsentWithdrawn(UUID userId, Instant withdrawnAt) {

}
