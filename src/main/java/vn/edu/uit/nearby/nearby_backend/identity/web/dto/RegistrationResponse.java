package vn.edu.uit.nearby.nearby_backend.identity.web.dto;

import java.util.UUID;

import vn.edu.uit.nearby.nearby_backend.identity.application.account.RegisteredAccount;
import vn.edu.uit.nearby.nearby_backend.identity.domain.AccountStatus;

public record RegistrationResponse(UUID userId, String email, AccountStatus status) {

	public static RegistrationResponse from(RegisteredAccount account) {
		return new RegistrationResponse(account.userId(), account.email(), account.status());
	}

}
