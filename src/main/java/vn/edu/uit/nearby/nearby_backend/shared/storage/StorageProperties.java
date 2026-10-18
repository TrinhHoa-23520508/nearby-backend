package vn.edu.uit.nearby.nearby_backend.shared.storage;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("app.storage")
public record StorageProperties(String endpoint, String publicEndpoint, String accessKey, String secretKey,
		String bucket, String region, Duration presignTtl) {
}
