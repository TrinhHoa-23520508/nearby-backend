package vn.edu.uit.nearby.nearby_backend.identity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import vn.edu.uit.nearby.nearby_backend.support.IntegrationTest;

@IntegrationTest
class AppParametersIntegrationTests {

	@Autowired
	AppParameters appParameters;

	@Test
	void readsSeededIntegerParameter() {
		assertThat(appParameters.getInt("area.max_count")).isEqualTo(2);
	}

	@Test
	void readsSeededDecimalParameter() {
		assertThat(appParameters.getDouble("reputation.gps_confirmed_weight")).isEqualTo(1.2);
	}

	@Test
	void unknownKeyFailsLoudly() {
		assertThatThrownBy(() -> appParameters.get("does.not.exist")).isInstanceOf(IllegalStateException.class)
			.hasMessageContaining("does.not.exist");
	}

}
