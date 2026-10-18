package vn.edu.uit.nearby.nearby_backend.identity.web.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import vn.edu.uit.nearby.nearby_backend.shared.geo.GeoPoint;

public record HomeAreaPayload(@NotNull @DecimalMin("-90") @DecimalMax("90") Double latitude,
		@NotNull @DecimalMin("-180") @DecimalMax("180") Double longitude) {

	public static HomeAreaPayload from(GeoPoint point) {
		return new HomeAreaPayload(point.latitude(), point.longitude());
	}

	public GeoPoint toGeoPoint() {
		return new GeoPoint(latitude, longitude);
	}

}
