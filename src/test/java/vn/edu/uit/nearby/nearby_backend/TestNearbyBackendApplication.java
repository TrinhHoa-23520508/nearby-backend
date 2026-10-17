package vn.edu.uit.nearby.nearby_backend;

import org.springframework.boot.SpringApplication;

public class TestNearbyBackendApplication {

	public static void main(String[] args) {
		SpringApplication.from(NearbyBackendApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
