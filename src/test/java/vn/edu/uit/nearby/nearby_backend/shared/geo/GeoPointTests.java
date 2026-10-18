package vn.edu.uit.nearby.nearby_backend.shared.geo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.locationtech.jts.geom.Point;

import vn.edu.uit.nearby.nearby_backend.shared.constant.GeoConstants;

class GeoPointTests {

	@Test
	void convertsToJtsPointWithLongitudeAsXAndWgs84Srid() {
		Point point = new GeoPoint(10.8700, 106.8031).toPoint();

		assertThat(point.getX()).isEqualTo(106.8031);
		assertThat(point.getY()).isEqualTo(10.8700);
		assertThat(point.getSRID()).isEqualTo(GeoConstants.WGS84_SRID);
	}

	@Test
	void roundTripsThroughJtsPoint() {
		GeoPoint uit = new GeoPoint(10.8700, 106.8031);

		assertThat(GeoPoint.from(uit.toPoint())).isEqualTo(uit);
	}

	@ParameterizedTest
	@CsvSource({ "90,180", "-90,-180", "0,0" })
	void acceptsCoordinatesOnTheValidBoundary(double latitude, double longitude) {
		assertThat(new GeoPoint(latitude, longitude).latitude()).isEqualTo(latitude);
	}

	@ParameterizedTest
	@CsvSource({ "90.0001,0", "-90.0001,0", "0,180.0001", "0,-180.0001" })
	void rejectsCoordinatesOutsideTheValidRange(double latitude, double longitude) {
		assertThatThrownBy(() -> new GeoPoint(latitude, longitude)).isInstanceOf(IllegalArgumentException.class);
	}

}
