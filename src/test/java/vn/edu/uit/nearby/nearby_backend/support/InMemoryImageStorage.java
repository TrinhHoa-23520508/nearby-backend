package vn.edu.uit.nearby.nearby_backend.support;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.boot.test.context.TestComponent;
import org.springframework.context.annotation.Primary;

import vn.edu.uit.nearby.nearby_backend.shared.storage.ImageStorage;

/**
 * Replaces MinIO in integration tests; {@link #simulateUpload} stands in for the client's
 * upload to the presigned URL.
 */
@Primary
@TestComponent
public class InMemoryImageStorage implements ImageStorage {

	private static final String BASE_URL = "https://storage.test/nearby-images/";

	private final Map<String, StoredObject> objects = new ConcurrentHashMap<>();

	@Override
	public PresignedUrl presignUpload(String objectKey, String contentType) {
		return new PresignedUrl(BASE_URL + objectKey + "?upload", "PUT", Instant.now().plus(Duration.ofMinutes(10)));
	}

	@Override
	public PresignedUrl presignDownload(String objectKey) {
		return new PresignedUrl(BASE_URL + objectKey, "GET", Instant.now().plus(Duration.ofMinutes(10)));
	}

	@Override
	public Optional<StoredObject> find(String objectKey) {
		return Optional.ofNullable(objects.get(objectKey));
	}

	@Override
	public void delete(String objectKey) {
		objects.remove(objectKey);
	}

	public void simulateUpload(String objectKey, String contentType, long sizeBytes) {
		objects.put(objectKey, new StoredObject(sizeBytes, contentType));
	}

	public boolean contains(String objectKey) {
		return objects.containsKey(objectKey);
	}

}
