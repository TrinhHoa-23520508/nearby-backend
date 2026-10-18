package vn.edu.uit.nearby.nearby_backend.shared.config;

import java.time.Clock;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration(proxyBeanMethods = false)
@EnableScheduling
@EnableAsync
public class PlatformConfig {

	@Bean
	Clock clock() {
		return Clock.systemUTC();
	}

}
