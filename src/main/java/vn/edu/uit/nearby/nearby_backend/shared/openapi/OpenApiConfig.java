package vn.edu.uit.nearby.nearby_backend.shared.openapi;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;

import vn.edu.uit.nearby.nearby_backend.shared.constant.SecurityConstants;

@Configuration(proxyBeanMethods = false)
public class OpenApiConfig {

	@Bean
	OpenAPI nearbyOpenApi() {
		return new OpenAPI()
			.info(new Info().title("Nearby API")
				.version("v1")
				.description("Hyperlocal second-hand marketplace. Errors follow RFC 9457 (application/problem+json); "
						+ "lists use cursor-based pagination; state-changing calls accept an Idempotency-Key header."))
			.components(new Components().addSecuritySchemes(SecurityConstants.OPENAPI_BEARER_SCHEME,
					new SecurityScheme().type(SecurityScheme.Type.HTTP).scheme("bearer").bearerFormat("JWT")))
			.addSecurityItem(new SecurityRequirement().addList(SecurityConstants.OPENAPI_BEARER_SCHEME));
	}

}
