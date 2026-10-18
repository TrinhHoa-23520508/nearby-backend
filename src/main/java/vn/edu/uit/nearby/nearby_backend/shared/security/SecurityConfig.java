package vn.edu.uit.nearby.nearby_backend.shared.security;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.web.servlet.HandlerExceptionResolver;

import vn.edu.uit.nearby.nearby_backend.shared.constant.ApiPaths;
import vn.edu.uit.nearby.nearby_backend.shared.constant.Roles;

@Configuration(proxyBeanMethods = false)
@EnableMethodSecurity
public class SecurityConfig {

	private static final String[] PUBLIC_ENDPOINTS = { ApiPaths.AUTH_PATTERN, ApiPaths.TEST_PATTERN,
			ApiPaths.WEBSOCKET_PATTERN, ApiPaths.ERROR, ApiPaths.ACTUATOR_HEALTH_PATTERN, ApiPaths.ACTUATOR_INFO,
			ApiPaths.ACTUATOR_PROMETHEUS, ApiPaths.OPENAPI_DOCS_PATTERN, ApiPaths.SWAGGER_UI_PATTERN,
			ApiPaths.SWAGGER_UI_PAGE };

	private static final String[] ADMIN_ENDPOINTS = { ApiPaths.ADMIN_PATTERN, ApiPaths.ACTUATOR_PATTERN };

	@Bean
	SecurityFilterChain securityFilterChain(HttpSecurity http, JwtAuthenticationConverter jwtAuthenticationConverter,
			@Qualifier("handlerExceptionResolver") HandlerExceptionResolver exceptionResolver) throws Exception {
		AuthenticationEntryPoint entryPoint = (request, response, ex) -> exceptionResolver.resolveException(request,
				response, null, ex);
		AccessDeniedHandler accessDeniedHandler = (request, response, ex) -> exceptionResolver.resolveException(request,
				response, null, ex);
		return http.csrf(AbstractHttpConfigurer::disable)
			.formLogin(AbstractHttpConfigurer::disable)
			.httpBasic(AbstractHttpConfigurer::disable)
			.logout(AbstractHttpConfigurer::disable)
			.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
			.authorizeHttpRequests(auth -> auth.requestMatchers(PUBLIC_ENDPOINTS)
				.permitAll()
				.requestMatchers(ADMIN_ENDPOINTS)
				.hasRole(Roles.ADMIN)
				.anyRequest()
				.authenticated())
			.oauth2ResourceServer(oauth -> oauth.jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter))
				.authenticationEntryPoint(entryPoint)
				.accessDeniedHandler(accessDeniedHandler))
			.exceptionHandling(
					handling -> handling.authenticationEntryPoint(entryPoint).accessDeniedHandler(accessDeniedHandler))
			.build();
	}

	@Bean
	PasswordEncoder passwordEncoder() {
		return PasswordEncoderFactories.createDelegatingPasswordEncoder();
	}

}
