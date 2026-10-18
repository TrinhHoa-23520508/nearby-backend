package vn.edu.uit.nearby.nearby_backend.identity.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;

import org.locationtech.jts.geom.Point;

import vn.edu.uit.nearby.nearby_backend.shared.geo.GeoPoint;

/**
 * The default area a user declares on the profile. It is rounded on creation so the
 * profile never stores a precise home location (NFR-18).
 */
public record HomeArea(GeoPoint center) {

	private static final int DECIMAL_PLACES = 3;

	public static HomeArea roundedFrom(GeoPoint point) {
		return new HomeArea(new GeoPoint(round(point.latitude()), round(point.longitude())));
	}

	public static HomeArea from(Point point) {
		return new HomeArea(GeoPoint.from(point));
	}

	public Point toPoint() {
		return center.toPoint();
	}

	private static double round(double coordinate) {
		return BigDecimal.valueOf(coordinate).setScale(DECIMAL_PLACES, RoundingMode.HALF_UP).doubleValue();
	}

}
