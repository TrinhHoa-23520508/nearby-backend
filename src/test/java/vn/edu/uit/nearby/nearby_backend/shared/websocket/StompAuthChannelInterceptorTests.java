package vn.edu.uit.nearby.nearby_backend.shared.websocket;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.security.Principal;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ExecutorSubscribableChannel;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.TestingAuthenticationToken;

import vn.edu.uit.nearby.nearby_backend.shared.constant.Roles;
import vn.edu.uit.nearby.nearby_backend.shared.constant.SecurityConstants;
import vn.edu.uit.nearby.nearby_backend.shared.security.JwtFixture;

class StompAuthChannelInterceptorTests {

	private final StompAuthChannelInterceptor interceptor = new StompAuthChannelInterceptor(JwtFixture.decoder(),
			JwtFixture.authenticationConverter());

	private final MessageChannel channel = new ExecutorSubscribableChannel();

	@Test
	void connectWithValidBearerTokenBindsTheUserToTheSession() {
		StompHeaderAccessor accessor = connect(SecurityConstants.BEARER_PREFIX + JwtFixture.accessToken(Roles.USER));

		interceptor.preSend(messageOf(accessor), channel);

		assertThat(accessor.getUser()).isNotNull();
	}

	@Test
	void connectWithoutAuthorizationHeaderIsRejected() {
		assertThatThrownBy(() -> interceptor.preSend(messageOf(connect(null)), channel))
			.isInstanceOf(BadCredentialsException.class);
	}

	@Test
	void connectWithNonBearerSchemeIsRejected() {
		assertThatThrownBy(() -> interceptor.preSend(messageOf(connect("Basic dXNlcjpwYXNz")), channel))
			.isInstanceOf(BadCredentialsException.class);
	}

	@Test
	void connectWithTamperedTokenIsRejected() {
		String tampered = JwtFixture.accessToken(Roles.USER) + "x";

		assertThatThrownBy(
				() -> interceptor.preSend(messageOf(connect(SecurityConstants.BEARER_PREFIX + tampered)), channel))
			.isInstanceOf(BadCredentialsException.class);
	}

	@Test
	void sendOnUnauthenticatedSessionIsRejected() {
		StompHeaderAccessor send = StompHeaderAccessor.create(StompCommand.SEND);

		assertThatThrownBy(() -> interceptor.preSend(messageOf(send), channel))
			.isInstanceOf(BadCredentialsException.class);
	}

	@Test
	void sendOnAuthenticatedSessionPassesThrough() {
		StompHeaderAccessor send = StompHeaderAccessor.create(StompCommand.SEND);
		Principal user = new TestingAuthenticationToken("user", null);
		send.setUser(user);
		Message<byte[]> message = messageOf(send);

		assertThat(interceptor.preSend(message, channel)).isSameAs(message);
	}

	@Test
	void disconnectIsAllowedWithoutAuthentication() {
		Message<byte[]> message = messageOf(StompHeaderAccessor.create(StompCommand.DISCONNECT));

		assertThat(interceptor.preSend(message, channel)).isSameAs(message);
	}

	private static StompHeaderAccessor connect(String authorizationHeader) {
		StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.CONNECT);
		if (authorizationHeader != null) {
			accessor.addNativeHeader(HttpHeaders.AUTHORIZATION, authorizationHeader);
		}
		accessor.setLeaveMutable(true);
		return accessor;
	}

	private static Message<byte[]> messageOf(StompHeaderAccessor accessor) {
		accessor.setLeaveMutable(true);
		return MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
	}

}
