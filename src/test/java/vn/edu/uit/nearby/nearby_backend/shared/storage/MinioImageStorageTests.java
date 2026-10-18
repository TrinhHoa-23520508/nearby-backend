package vn.edu.uit.nearby.nearby_backend.shared.storage;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;

import org.junit.jupiter.api.Test;

import vn.edu.uit.nearby.nearby_backend.shared.storage.ImageStorage.PresignedUrl;

class MinioImageStorageTests {

	private static final Instant NOW = Instant.parse("2026-10-18T00:00:00Z");

	private final StorageProperties properties = new StorageProperties("http://minio:9000",
			"https://storage.example.com", "access", "secret-key", "nearby-images", "us-east-1",
			Duration.ofMinutes(10));

	private final ImageStorage storage = new MinioImageStorage(properties, Clock.fixed(NOW, ZoneOffset.UTC));

	@Test
	void uploadUrlIsSignedForThePublicHostNotTheInternalOne() {
		PresignedUrl upload = storage.presignUpload("listings/abc/1.jpg", "image/jpeg");

		URI uri = URI.create(upload.url());
		assertThat(uri.getHost()).isEqualTo("storage.example.com");
		assertThat(uri.getPath()).isEqualTo("/nearby-images/listings/abc/1.jpg");
		assertThat(uri.getQuery()).contains("X-Amz-Signature=", "X-Amz-Expires=600");
		assertThat(upload.method()).isEqualTo("PUT");
	}

	@Test
	void downloadUrlUsesGetAndExpiresAfterConfiguredTtl() {
		PresignedUrl download = storage.presignDownload("listings/abc/1.jpg");

		assertThat(download.method()).isEqualTo("GET");
		assertThat(download.expiresAt()).isEqualTo(NOW.plus(Duration.ofMinutes(10)));
	}

}
