package vn.edu.uit.nearby.nearby_backend.identity.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import vn.edu.uit.nearby.nearby_backend.shared.error.InvalidRequestException;

class ImageUploadPolicyTests {

	private static final long FIVE_MEGABYTES = 5L * 1024 * 1024;

	private final ImageUploadPolicy policy = new ImageUploadPolicy(FIVE_MEGABYTES);

	@ParameterizedTest
	@CsvSource({ "image/jpeg,jpg", "image/png,png", "image/webp,webp" })
	void acceptsJpegPngAndWebpUpToTheLimit(String contentType, String extension) {
		assertThatCode(() -> policy.validate(contentType, FIVE_MEGABYTES)).doesNotThrowAnyException();
		assertThat(policy.fileExtensionOf(contentType)).isEqualTo(extension);
	}

	@ParameterizedTest
	@ValueSource(strings = { "image/gif", "application/pdf", "text/plain" })
	void rejectsOtherFormats(String contentType) {
		assertThatThrownBy(() -> policy.validate(contentType, 1)).isInstanceOf(InvalidRequestException.class)
			.hasMessageContaining("JPEG, PNG and WebP");
	}

	@Test
	void rejectsMissingContentType() {
		assertThatThrownBy(() -> policy.validate(null, 1)).isInstanceOf(InvalidRequestException.class);
	}

	@Test
	void rejectsOneByteOverTheLimit() {
		assertThatThrownBy(() -> policy.validate("image/png", FIVE_MEGABYTES + 1))
			.isInstanceOf(InvalidRequestException.class)
			.hasMessageContaining("5 MB");
	}

	@Test
	void rejectsAnEmptyImage() {
		assertThatThrownBy(() -> policy.validate("image/png", 0)).isInstanceOf(InvalidRequestException.class);
	}

}
