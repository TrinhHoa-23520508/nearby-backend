package vn.edu.uit.nearby.nearby_backend.identity.web.dto;

import java.time.Instant;
import java.util.Map;

import vn.edu.uit.nearby.nearby_backend.identity.application.profile.AvatarUpload;

public record AvatarUploadUrlResponse(String objectKey, String uploadUrl, String method, Instant expiresAt,
		Map<String, String> requiredHeaders) {

	public static AvatarUploadUrlResponse from(AvatarUpload upload) {
		return new AvatarUploadUrlResponse(upload.objectKey(), upload.uploadUrl(), upload.method(), upload.expiresAt(),
				upload.requiredHeaders());
	}

}
