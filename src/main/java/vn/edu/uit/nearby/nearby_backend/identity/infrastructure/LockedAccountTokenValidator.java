package vn.edu.uit.nearby.nearby_backend.identity.infrastructure;

import java.util.UUID;

import org.springframework.dao.DataAccessException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2ErrorCodes;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

import vn.edu.uit.nearby.nearby_backend.identity.application.port.LockedAccountRegistry;

@Slf4j
@Component
public class LockedAccountTokenValidator implements OAuth2TokenValidator<Jwt> {

	private static final OAuth2Error ACCOUNT_LOCKED = new OAuth2Error(OAuth2ErrorCodes.INVALID_TOKEN,
			"The account is locked", null);

	private final LockedAccountRegistry lockedAccounts;

	public LockedAccountTokenValidator(LockedAccountRegistry lockedAccounts) {
		this.lockedAccounts = lockedAccounts;
	}

	// Fails open: while Redis is down, tokens of a locked account stay valid until they
	// expire (at most one access token TTL), which keeps the API available (SRS 5.2).
	@Override
	public OAuth2TokenValidatorResult validate(Jwt jwt) {
		try {
			if (lockedAccounts.isLocked(UUID.fromString(jwt.getSubject()))) {
				return OAuth2TokenValidatorResult.failure(ACCOUNT_LOCKED);
			}
		}
		catch (DataAccessException ex) {
			log.warn("Locked-account check skipped, Redis unavailable: {}", ex.getMessage());
		}
		return OAuth2TokenValidatorResult.success();
	}

}
