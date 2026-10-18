package vn.edu.uit.nearby.nearby_backend.shared.web;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import vn.edu.uit.nearby.nearby_backend.shared.constant.ApiPaths;
import vn.edu.uit.nearby.nearby_backend.shared.web.idempotency.IdempotencyInterceptor;

@Configuration(proxyBeanMethods = false)
public class WebMvcConfig implements WebMvcConfigurer {

	private final IdempotencyInterceptor idempotencyInterceptor;

	public WebMvcConfig(IdempotencyInterceptor idempotencyInterceptor) {
		this.idempotencyInterceptor = idempotencyInterceptor;
	}

	@Override
	public void addInterceptors(InterceptorRegistry registry) {
		registry.addInterceptor(idempotencyInterceptor).addPathPatterns(ApiPaths.API_PATTERN);
	}

}
