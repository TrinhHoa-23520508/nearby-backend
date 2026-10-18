package vn.edu.uit.nearby.nearby_backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.modulith.Modulithic;

@Modulithic(systemName = "Nearby", sharedModules = "shared")
@SpringBootApplication
@ConfigurationPropertiesScan
public class NearbyBackendApplication {

	public static void main(String[] args) {
		SpringApplication.run(NearbyBackendApplication.class, args);
	}

}
