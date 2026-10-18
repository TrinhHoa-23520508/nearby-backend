package vn.edu.uit.nearby.nearby_backend.shared.websocket;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

import vn.edu.uit.nearby.nearby_backend.shared.constant.ApiPaths;
import vn.edu.uit.nearby.nearby_backend.shared.constant.WebSocketDestinations;

@Configuration(proxyBeanMethods = false)
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

	private final StompAuthChannelInterceptor stompAuthChannelInterceptor;

	public WebSocketConfig(StompAuthChannelInterceptor stompAuthChannelInterceptor) {
		this.stompAuthChannelInterceptor = stompAuthChannelInterceptor;
	}

	@Override
	public void registerStompEndpoints(StompEndpointRegistry registry) {
		// Native Android client only: there is no browser Origin to restrict.
		registry.addEndpoint(ApiPaths.WEBSOCKET_ENDPOINT).setAllowedOriginPatterns("*");
	}

	@Override
	public void configureMessageBroker(MessageBrokerRegistry registry) {
		// Single-instance broker; Architecture Design 8.1 covers the relay option.
		registry.enableSimpleBroker(WebSocketDestinations.TOPIC_PREFIX, WebSocketDestinations.QUEUE_PREFIX);
		registry.setApplicationDestinationPrefixes(WebSocketDestinations.APPLICATION_PREFIX);
		registry.setUserDestinationPrefix(WebSocketDestinations.USER_PREFIX);
	}

	@Override
	public void configureClientInboundChannel(ChannelRegistration registration) {
		registration.interceptors(stompAuthChannelInterceptor);
	}

}
