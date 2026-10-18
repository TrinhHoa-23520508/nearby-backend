package vn.edu.uit.nearby.nearby_backend.identity.application.profile;

import java.util.UUID;

import vn.edu.uit.nearby.nearby_backend.shared.error.InvalidRequestException;

public interface ProfileService {

	UserProfile getProfile(UUID userId);

	UserProfile updateProfile(UUID userId, UpdateProfileCommand command);

	/**
	 * Step 1 of an avatar change: returns a presigned URL to upload the image directly to
	 * storage.
	 * @throws InvalidRequestException {@code INVALID_IMAGE} for a format other than JPEG,
	 * PNG or WebP, or a size over the limit
	 */
	AvatarUpload createAvatarUpload(UUID userId, AvatarUploadCommand command);

	/**
	 * Step 2: checks the uploaded object and makes it the avatar. An invalid upload is
	 * deleted.
	 * @throws InvalidRequestException {@code INVALID_AVATAR_KEY},
	 * {@code AVATAR_NOT_UPLOADED} or {@code INVALID_IMAGE}
	 */
	UserProfile confirmAvatar(UUID userId, String objectKey);

	void removeAvatar(UUID userId);

}
