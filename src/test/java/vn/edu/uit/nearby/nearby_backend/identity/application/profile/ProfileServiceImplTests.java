package vn.edu.uit.nearby.nearby_backend.identity.application.profile;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static vn.edu.uit.nearby.nearby_backend.identity.domain.UserFixtures.NOW;

import java.time.Clock;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import vn.edu.uit.nearby.nearby_backend.identity.application.IdentityParametersFixture;
import vn.edu.uit.nearby.nearby_backend.identity.domain.User;
import vn.edu.uit.nearby.nearby_backend.identity.domain.UserFixtures;
import vn.edu.uit.nearby.nearby_backend.identity.infrastructure.UserRepository;
import vn.edu.uit.nearby.nearby_backend.shared.error.BusinessException;
import vn.edu.uit.nearby.nearby_backend.shared.error.InvalidRequestException;
import vn.edu.uit.nearby.nearby_backend.shared.geo.GeoPoint;
import vn.edu.uit.nearby.nearby_backend.shared.storage.ImageStorage;
import vn.edu.uit.nearby.nearby_backend.shared.storage.ImageStorage.PresignedUrl;
import vn.edu.uit.nearby.nearby_backend.shared.storage.ImageStorage.StoredObject;

@ExtendWith(MockitoExtension.class)
class ProfileServiceImplTests {

	private static final long FIVE_MEGABYTES = 5L * 1024 * 1024;

	@Mock
	UserRepository users;

	@Mock
	ImageStorage storage;

	@Mock
	ApplicationEventPublisher events;

	private final User user = UserFixtures.activeUser();

	private ProfileServiceImpl profileService;

	@BeforeEach
	void createService() {
		profileService = new ProfileServiceImpl(users, storage, IdentityParametersFixture.seeded(), events,
				Clock.fixed(NOW, ZoneOffset.UTC));
	}

	@Test
	void profileWithoutAvatarOrAreaHasNeither() {
		givenUserExists();

		UserProfile profile = profileService.getProfile(user.getId());

		assertThat(profile.email()).isEqualTo(UserFixtures.EMAIL);
		assertThat(profile.avatarUrl()).isNull();
		assertThat(profile.homeArea()).isNull();
		assertThat(profile.emailVerified()).isTrue();
		verifyNoInteractions(storage);
	}

	@Test
	void avatarIsReturnedAsAPresignedDownloadUrl() {
		givenUserExists();
		user.replaceAvatar("avatars/a.jpg", NOW);
		when(storage.presignDownload("avatars/a.jpg")).thenReturn(new PresignedUrl("https://cdn/a.jpg", "GET", NOW));

		assertThat(profileService.getProfile(user.getId()).avatarUrl()).isEqualTo("https://cdn/a.jpg");
	}

	@Test
	void updateTrimsTheNameAndRoundsTheHomeArea() {
		givenUserExists();

		UserProfile profile = profileService.updateProfile(user.getId(),
				new UpdateProfileCommand("  Binh Tran ", new GeoPoint(10.870149, 106.803151)));

		assertThat(profile.displayName()).isEqualTo("Binh Tran");
		assertThat(profile.homeArea()).isEqualTo(new GeoPoint(10.870, 106.803));
	}

	@Test
	void updateWithoutHomeAreaClearsIt() {
		givenUserExists();
		profileService.updateProfile(user.getId(), new UpdateProfileCommand("An", new GeoPoint(10.87, 106.8)));

		UserProfile profile = profileService.updateProfile(user.getId(), new UpdateProfileCommand("An", null));

		assertThat(profile.homeArea()).isNull();
	}

	@Test
	void uploadUrlIsIssuedForAnObjectKeyInTheUsersAvatarFolder() {
		when(storage.presignUpload(anyString(), any()))
			.thenAnswer(call -> new PresignedUrl("https://upload/" + call.getArgument(0), "PUT", NOW));

		AvatarUpload upload = profileService.createAvatarUpload(user.getId(),
				new AvatarUploadCommand("image/webp", 1024));

		assertThat(upload.objectKey()).matches("avatars/" + user.getId() + "/[0-9a-f-]{36}\\.webp");
		assertThat(upload.uploadUrl()).isEqualTo("https://upload/" + upload.objectKey());
		assertThat(upload.method()).isEqualTo("PUT");
		assertThat(upload.requiredHeaders()).isEqualTo(Map.of("Content-Type", "image/webp"));
	}

	@Test
	void uploadUrlIsRefusedForAnInvalidImage() {
		assertThatThrownBy(
				() -> profileService.createAvatarUpload(user.getId(), new AvatarUploadCommand("image/gif", 1024)))
			.isInstanceOf(InvalidRequestException.class);
		verifyNoInteractions(storage);
	}

	@ParameterizedTest
	@ValueSource(strings = { "avatars/someone-else/0b6c7a3e-8a64-4c4c-9a55-0e1f0c3b9e11.jpg",
			"listings/x/0b6c7a3e-8a64-4c4c-9a55-0e1f0c3b9e11.jpg", "{own}../0b6c7a3e-8a64-4c4c-9a55-0e1f0c3b9e11.jpg",
			"{own}0b6c7a3e-8a64-4c4c-9a55-0e1f0c3b9e11.gif" })
	void keyNotIssuedForTheUsersAvatarIsRejected(String objectKey) {
		String key = objectKey.replace("{own}", "avatars/" + user.getId() + "/");

		assertThatThrownBy(() -> profileService.confirmAvatar(user.getId(), key)).extracting(this::codeOf)
			.isEqualTo("INVALID_AVATAR_KEY");
		verifyNoInteractions(storage);
	}

	@Test
	void confirmingBeforeUploadingIsRejected() {
		String key = ownAvatarKey();
		when(storage.find(key)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> profileService.confirmAvatar(user.getId(), key)).extracting(this::codeOf)
			.isEqualTo("AVATAR_NOT_UPLOADED");
	}

	@Test
	void uploadedImageOverTheLimitIsDeletedAndRejected() {
		String key = ownAvatarKey();
		when(storage.find(key)).thenReturn(Optional.of(new StoredObject(FIVE_MEGABYTES + 1, "image/jpeg")));

		assertThatThrownBy(() -> profileService.confirmAvatar(user.getId(), key)).extracting(this::codeOf)
			.isEqualTo("INVALID_IMAGE");
		verify(storage).delete(key);
		verify(users, never()).getRequired(any());
	}

	@Test
	void confirmedAvatarReplacesThePreviousOneWhichIsDiscardedAfterCommit() {
		givenUserExists();
		user.replaceAvatar("avatars/old.jpg", NOW);
		String key = ownAvatarKey();
		when(storage.find(key)).thenReturn(Optional.of(new StoredObject(FIVE_MEGABYTES, "image/jpeg")));
		when(storage.presignDownload(key)).thenReturn(new PresignedUrl("https://cdn/new.jpg", "GET", NOW));

		UserProfile profile = profileService.confirmAvatar(user.getId(), key);

		assertThat(profile.avatarUrl()).isEqualTo("https://cdn/new.jpg");
		verify(events).publishEvent(new AvatarDiscarded("avatars/old.jpg"));
		verify(storage, never()).delete(anyString());
	}

	@Test
	void removingTheAvatarDiscardsIt() {
		givenUserExists();
		user.replaceAvatar("avatars/a.jpg", NOW);

		profileService.removeAvatar(user.getId());

		assertThat(user.getAvatarKey()).isNull();
		verify(events).publishEvent(new AvatarDiscarded("avatars/a.jpg"));
	}

	@Test
	void removingAMissingAvatarPublishesNothing() {
		givenUserExists();

		profileService.removeAvatar(user.getId());

		verifyNoInteractions(events);
	}

	@Test
	void discardedAvatarIsDeletedFromStorage() {
		new AvatarCleanupListener(storage).deleteDiscardedAvatar(new AvatarDiscarded("avatars/a.jpg"));

		verify(storage).delete("avatars/a.jpg");
	}

	private void givenUserExists() {
		when(users.getRequired(user.getId())).thenReturn(user);
	}

	private String ownAvatarKey() {
		return "avatars/" + user.getId() + "/" + UUID.randomUUID() + ".jpg";
	}

	private String codeOf(Throwable ex) {
		return ((BusinessException) ex).getCode();
	}

}
