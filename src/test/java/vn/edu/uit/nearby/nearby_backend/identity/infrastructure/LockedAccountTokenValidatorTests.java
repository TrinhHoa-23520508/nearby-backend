package vn.edu.uit.nearby.nearby_backend.identity.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.security.oauth2.jwt.Jwt;

import vn.edu.uit.nearby.nearby_backend.identity.application.port.LockedAccountRegistry;

@ExtendWith(MockitoExtension.class)
class LockedAccountTokenValidatorTests {

	private final UUID userId = UUID.randomUUID();

	@Mock
	LockedAccountRegistry lockedAccounts;

	@Test
	void tokenOfALockedAccountIsRejected() {
		when(lockedAccounts.isLocked(userId)).thenReturn(true);

		assertThat(validator().validate(jwtOf(userId)).hasErrors()).isTrue();
	}

	@Test
	void tokenOfAnUnlockedAccountIsAccepted() {
		when(lockedAccounts.isLocked(userId)).thenReturn(false);

		assertThat(validator().validate(jwtOf(userId)).hasErrors()).isFalse();
	}

	@Test
	void checkFailsOpenWhenRedisIsDown() {
		when(lockedAccounts.isLocked(userId)).thenThrow(new RedisConnectionFailureException("down"));

		assertThat(validator().validate(jwtOf(userId)).hasErrors()).isFalse();
	}

	private LockedAccountTokenValidator validator() {
		return new LockedAccountTokenValidator(lockedAccounts);
	}

	private static Jwt jwtOf(UUID subject) {
		return Jwt.withTokenValue("token")
			.header("alg", "HS256")
			.subject(subject.toString())
			.issuedAt(Instant.now())
			.expiresAt(Instant.now().plusSeconds(60))
			.build();
	}

}
