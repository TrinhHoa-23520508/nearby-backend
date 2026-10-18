package vn.edu.uit.nearby.nearby_backend.shared.geo;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.PrecisionModel;

import vn.edu.uit.nearby.nearby_backend.shared.constant.GeoConstants;

public record GeoPoint(double latitude, double longitude) {

	private static final GeometryFactory WGS84 = new GeometryFactory(new PrecisionModel(), GeoConstants.WGS84_SRID);

	public GeoPoint {
		if (latitude < -90 || latitude > 90) {
			throw new IllegalArgumentException("Latitude out of range: " + latitude);
		}
		if (longitude < -180 || longitude > 180) {
			throw new IllegalArgumentException("Longitude out of range: " + longitude);
		}
	}

	public static GeoPoint from(Point point) {
		return new GeoPoint(point.getY(), point.getX());
	}

	public Point toPoint() {
		// JTS orders coordinates as (x = longitude, y = latitude).
		return WGS84.createPoint(new Coordinate(longitude, latitude));
	}

}
