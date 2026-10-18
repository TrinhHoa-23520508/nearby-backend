package vn.edu.uit.nearby.nearby_backend.shared.constant;

import java.util.Map;

import org.springframework.http.MediaType;

public final class ImageContentTypes {

	public static final String JPEG = MediaType.IMAGE_JPEG_VALUE;

	public static final String PNG = MediaType.IMAGE_PNG_VALUE;

	public static final String WEBP = "image/webp";

	public static final Map<String, String> FILE_EXTENSIONS = Map.of(JPEG, "jpg", PNG, "png", WEBP, "webp");

	private ImageContentTypes() {
	}

}
