package vn.edu.uit.nearby.nearby_backend.shared.storage;

import java.time.Clock;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;

import io.minio.BucketExistsArgs;
import io.minio.GetPresignedObjectUrlArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.RemoveObjectArgs;
import io.minio.StatObjectArgs;
import io.minio.StatObjectResponse;
import io.minio.errors.ErrorResponseException;
import io.minio.http.Method;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class MinioImageStorage implements ImageStorage {

	private static final String NO_SUCH_KEY = "NoSuchKey";

	private final StorageProperties properties;

	private final Clock clock;

	private final MinioClient internalClient;

	// Presigned signatures are bound to the host the app will call.
	private final MinioClient presignClient;

	public MinioImageStorage(StorageProperties properties, Clock clock) {
		this.properties = properties;
		this.clock = clock;
		this.internalClient = clientFor(properties.endpoint());
		this.presignClient = clientFor(properties.publicEndpoint());
	}

	@Override
	public PresignedUrl presignUpload(String objectKey, String contentType) {
		return presign(Method.PUT, objectKey, Map.of(HttpHeaders.CONTENT_TYPE, contentType));
	}

	@Override
	public PresignedUrl presignDownload(String objectKey) {
		return presign(Method.GET, objectKey, Map.of());
	}

	@Override
	public Optional<StoredObject> find(String objectKey) {
		try {
			StatObjectResponse stat = internalClient
				.statObject(StatObjectArgs.builder().bucket(properties.bucket()).object(objectKey).build());
			return Optional.of(new StoredObject(stat.size(), stat.contentType()));
		}
		catch (ErrorResponseException ex) {
			if (NO_SUCH_KEY.equals(ex.errorResponse().code())) {
				return Optional.empty();
			}
			throw new StorageException("Failed to read metadata of " + objectKey, ex);
		}
		catch (Exception ex) {
			throw new StorageException("Failed to read metadata of " + objectKey, ex);
		}
	}

	@Override
	public void delete(String objectKey) {
		try {
			internalClient
				.removeObject(RemoveObjectArgs.builder().bucket(properties.bucket()).object(objectKey).build());
		}
		catch (Exception ex) {
			throw new StorageException("Failed to delete " + objectKey, ex);
		}
	}

	// Not fatal: the API must still start while storage is temporarily unreachable.
	@EventListener(ApplicationReadyEvent.class)
	void createBucketIfMissing() {
		String bucket = properties.bucket();
		try {
			if (!internalClient.bucketExists(BucketExistsArgs.builder().bucket(bucket).build())) {
				internalClient.makeBucket(MakeBucketArgs.builder().bucket(bucket).build());
				log.info("Created image bucket '{}'", bucket);
			}
		}
		catch (Exception ex) {
			log.warn("Image storage not reachable at {}: {}", properties.endpoint(), ex.getMessage());
		}
	}

	private PresignedUrl presign(Method method, String objectKey, Map<String, String> headers) {
		int ttlSeconds = (int) properties.presignTtl().toSeconds();
		try {
			String url = presignClient.getPresignedObjectUrl(GetPresignedObjectUrlArgs.builder()
				.method(method)
				.bucket(properties.bucket())
				.object(objectKey)
				.expiry(ttlSeconds, TimeUnit.SECONDS)
				.extraHeaders(headers)
				.build());
			return new PresignedUrl(url, method.name(), clock.instant().plusSeconds(ttlSeconds));
		}
		catch (Exception ex) {
			throw new StorageException("Failed to presign " + method + " for " + objectKey, ex);
		}
	}

	private MinioClient clientFor(String endpoint) {
		return MinioClient.builder()
			.endpoint(endpoint)
			.region(properties.region())
			.credentials(properties.accessKey(), properties.secretKey())
			.build();
	}

}
