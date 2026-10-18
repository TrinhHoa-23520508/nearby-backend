package vn.edu.uit.nearby.nearby_backend.identity.application.account;

import java.util.UUID;

import vn.edu.uit.nearby.nearby_backend.identity.domain.AccountStatus;
import vn.edu.uit.nearby.nearby_backend.identity.domain.User;

public record RegisteredAccount(UUID userId, String email, AccountStatus status) {

	static RegisteredAccount of(User user) {
		return new RegisteredAccount(user.getId(), user.getEmail(), user.getStatus());
	}

}
