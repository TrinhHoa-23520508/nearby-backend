package vn.edu.uit.nearby.nearby_backend.identity.application.profile;

import java.time.Clock;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import vn.edu.uit.nearby.nearby_backend.identity.application.IdentityParameters;
import vn.edu.uit.nearby.nearby_backend.identity.domain.HomeArea;
import vn.edu.uit.nearby.nearby_backend.identity.domain.ImageUploadPolicy;
import vn.edu.uit.nearby.nearby_backend.identity.domain.User;
import vn.edu.uit.nearby.nearby_backend.identity.infrastructure.UserRepository;
import vn.edu.uit.nearby.nearby_backend.shared.constant.ErrorCodes;
import vn.edu.uit.nearby.nearby_backend.shared.constant.StorageKeyPrefixes;
import vn.edu.uit.nearby.nearby_backend.shared.error.InvalidRequestException;
import vn.edu.uit.nearby.nearby_backend.shared.geo.GeoPoint;
import vn.edu.uit.nearby.nearby_backend.shared.storage.ImageStorage;
import vn.edu.uit.nearby.nearby_backend.shared.storage.ImageStorage.PresignedUrl;
import vn.edu.uit.nearby.nearby_backend.shared.storage.ImageStorage.StoredObject;

@Service
public class ProfileServiceImpl implements ProfileService {

	private static final Pattern AVATAR_FILE_NAME = Pattern.compile("[0-9a-f-]{36}\\.(jpg|png|webp)");

	private final UserRepository users;

	private final ImageStorage storage;

	private final IdentityParameters parameters;

	private final ApplicationEventPublisher events;

	private final Clock clock;

	public ProfileServiceImpl(UserRepository users, ImageStorage storage, IdentityParameters parameters,
			ApplicationEventPublisher events, Clock clock) {
		this.users = users;
		this.storage = storage;
		this.parameters = parameters;
		this.events = events;
		this.clock = clock;
	}

	@Override
	@Transactional(readOnly = true)
	public UserProfile getProfile(UUID userId) {
		return toProfile(users.getRequired(userId));
	}

	@Override
	@Transactional
	public UserProfile updateProfile(UUID userId, UpdateProfileCommand command) {
		User user = users.getRequired(userId);
		HomeArea homeArea = command.homeArea() == null ? null : HomeArea.roundedFrom(command.homeArea());
		user.updateProfile(command.displayName().trim(), homeArea, clock.instant());
		return toProfile(user);
	}

	@Override
	public AvatarUpload createAvatarUpload(UUID userId, AvatarUploadCommand command) {
		ImageUploadPolicy policy = imageUploadPolicy();
		policy.validate(command.contentType(), command.sizeBytes());
		String objectKey = avatarFolderOf(userId) + UUID.randomUUID() + "."
				+ policy.fileExtensionOf(command.contentType());
		PresignedUrl upload = storage.presignUpload(objectKey, command.contentType());
		return new AvatarUpload(objectKey, upload.url(), upload.method(), upload.expiresAt(),
				Map.of(HttpHeaders.CONTENT_TYPE, command.contentType()));
	}

	@Override
	@Transactional
	public UserProfile confirmAvatar(UUID userId, String objectKey) {
		requireOwnAvatarKey(userId, objectKey);
		StoredObject uploaded = storage.find(objectKey)
			.orElseThrow(() -> new InvalidRequestException(ErrorCodes.AVATAR_NOT_UPLOADED,
					"No image was uploaded for this key."));
		requireValidImage(objectKey, uploaded);
		User user = users.getRequired(userId);
		user.replaceAvatar(objectKey, clock.instant()).ifPresent(this::discard);
		return toProfile(user);
	}

	@Override
	@Transactional
	public void removeAvatar(UUID userId) {
		users.getRequired(userId).removeAvatar(clock.instant()).ifPresent(this::discard);
	}

	private void requireOwnAvatarKey(UUID userId, String objectKey) {
		String folder = avatarFolderOf(userId);
		boolean isOwnAvatarKey = objectKey.startsWith(folder)
				&& AVATAR_FILE_NAME.matcher(objectKey.substring(folder.length())).matches();
		if (!isOwnAvatarKey) {
			throw new InvalidRequestException(ErrorCodes.INVALID_AVATAR_KEY,
					"The object key was not issued for your avatar.");
		}
	}

	private void requireValidImage(String objectKey, StoredObject uploaded) {
		try {
			imageUploadPolicy().validate(uploaded.contentType(), uploaded.sizeBytes());
		}
		catch (InvalidRequestException ex) {
			storage.delete(objectKey);
			throw ex;
		}
	}

	private void discard(String objectKey) {
		events.publishEvent(new AvatarDiscarded(objectKey));
	}

	private ImageUploadPolicy imageUploadPolicy() {
		return new ImageUploadPolicy(parameters.imageMaxBytes());
	}

	private UserProfile toProfile(User user) {
		String avatarUrl = user.getAvatarKey() == null ? null : storage.presignDownload(user.getAvatarKey()).url();
		GeoPoint homeArea = user.homeArea().map(HomeArea::center).orElse(null);
		return new UserProfile(user.getId(), user.getEmail(), user.getDisplayName(), avatarUrl, user.getRole(),
				user.getStatus(), homeArea, user.hasVerifiedEmail(), user.getCreatedAt());
	}

	private static String avatarFolderOf(UUID userId) {
		return StorageKeyPrefixes.AVATARS + userId + "/";
	}

}
