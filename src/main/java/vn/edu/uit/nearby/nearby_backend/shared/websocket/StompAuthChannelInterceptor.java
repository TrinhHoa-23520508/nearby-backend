package vn.edu.uit.nearby.nearby_backend.shared.websocket;

import org.springframework.http.HttpHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.stereotype.Component;

import vn.edu.uit.nearby.nearby_backend.shared.constant.SecurityConstants;

@Component
public class StompAuthChannelInterceptor implements ChannelInterceptor {

	private final JwtDecoder jwtDecoder;

	private final JwtAuthenticationConverter jwtAuthenticationConverter;

	public StompAuthChannelInterceptor(JwtDecoder jwtDecoder, JwtAuthenticationConverter jwtAuthenticationConverter) {
		this.jwtDecoder = jwtDecoder;
		this.jwtAuthenticationConverter = jwtAuthenticationConverter;
	}

	@Override
	public Message<?> preSend(Message<?> message, MessageChannel channel) {
		StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
		if (accessor == null || accessor.getCommand() == null) {
			return message;
		}
		if (accessor.getCommand() == StompCommand.CONNECT) {
			accessor.setUser(authenticate(accessor.getFirstNativeHeader(HttpHeaders.AUTHORIZATION)));
			return message;
		}
		if (accessor.getCommand() != StompCommand.DISCONNECT && accessor.getUser() == null) {
			throw new BadCredentialsException("STOMP session is not authenticated");
		}
		return message;
	}

	private Authentication authenticate(String authorizationHeader) {
		if (authorizationHeader == null || !authorizationHeader.startsWith(SecurityConstants.BEARER_PREFIX)) {
			throw new BadCredentialsException("Missing bearer token in STOMP CONNECT");
		}
		try {
			return jwtAuthenticationConverter
				.convert(jwtDecoder.decode(authorizationHeader.substring(SecurityConstants.BEARER_PREFIX.length())));
		}
		catch (JwtException ex) {
			throw new BadCredentialsException("Invalid access token", ex);
		}
	}

}
