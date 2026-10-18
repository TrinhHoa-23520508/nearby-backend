package vn.edu.uit.nearby.nearby_backend.identity.domain;

import vn.edu.uit.nearby.nearby_backend.shared.constant.ErrorCodes;
import vn.edu.uit.nearby.nearby_backend.shared.constant.ImageContentTypes;
import vn.edu.uit.nearby.nearby_backend.shared.error.InvalidRequestException;

/**
 * Accepted image formats and size (FR-ID-03). Checked once when the client asks for an
 * upload URL and again on the uploaded object, because a presigned URL cannot limit size.
 */
public record ImageUploadPolicy(long maxSizeBytes) {

	private static final long BYTES_PER_MEGABYTE = 1024 * 1024;

	public void validate(String contentType, long sizeBytes) {
		if (contentType == null || !ImageContentTypes.FILE_EXTENSIONS.containsKey(contentType)) {
			throw new InvalidRequestException(ErrorCodes.INVALID_IMAGE, "Only JPEG, PNG and WebP images are accepted.");
		}
		if (sizeBytes <= 0 || sizeBytes > maxSizeBytes) {
			throw new InvalidRequestException(ErrorCodes.INVALID_IMAGE,
					"The image must be at most " + maxSizeBytes / BYTES_PER_MEGABYTE + " MB.");
		}
	}

	public String fileExtensionOf(String contentType) {
		return ImageContentTypes.FILE_EXTENSIONS.get(contentType);
	}

}
