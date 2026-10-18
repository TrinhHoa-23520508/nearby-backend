package vn.edu.uit.nearby.nearby_backend.shared.storage;

import java.time.Instant;

/**
 * Images never pass through the backend: clients upload and download with presigned URLs.
 * Every method throws {@link StorageException} when the storage backend fails.
 */
public interface ImageStorage {

	PresignedUrl presignUpload(String objectKey, String contentType);

	PresignedUrl presignDownload(String objectKey);

	void delete(String objectKey);

	record PresignedUrl(String url, String method, Instant expiresAt) {
	}

}
