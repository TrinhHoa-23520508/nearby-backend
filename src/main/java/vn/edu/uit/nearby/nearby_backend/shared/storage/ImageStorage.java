package vn.edu.uit.nearby.nearby_backend.shared.storage;

import java.time.Instant;
import java.util.Optional;

/**
 * Images never pass through the backend: clients upload and download with presigned URLs.
 * Every method throws {@link StorageException} when the storage backend fails.
 */
public interface ImageStorage {

	PresignedUrl presignUpload(String objectKey, String contentType);

	PresignedUrl presignDownload(String objectKey);

	/**
	 * Returns the metadata of an uploaded object, or empty when nothing was uploaded
	 * under that key. Used to check what the client actually uploaded with a presigned
	 * URL.
	 */
	Optional<StoredObject> find(String objectKey);

	void delete(String objectKey);

	record PresignedUrl(String url, String method, Instant expiresAt) {
	}

	record StoredObject(long sizeBytes, String contentType) {
	}

}
