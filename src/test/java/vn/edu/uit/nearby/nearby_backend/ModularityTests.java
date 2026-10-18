package vn.edu.uit.nearby.nearby_backend;

import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;
import org.springframework.modulith.docs.Documenter;

class ModularityTests {

	private final ApplicationModules modules = ApplicationModules.of(NearbyBackendApplication.class);

	@Test
	void verifiesModuleStructure() {
		modules.verify();
	}

	@Test
	void writesDocumentation() {
		new Documenter(modules).writeDocumentation();
	}

}
