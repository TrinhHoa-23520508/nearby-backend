package vn.edu.uit.nearby.nearby_backend.identity.application.profile;

import java.time.Instant;
import java.util.Map;

/**
 * @param requiredHeaders headers the client must send with the upload, because they are
 * part of the signature
 */
public record AvatarUpload(String objectKey, String uploadUrl, String method, Instant expiresAt,
		Map<String, String> requiredHeaders) {

}
