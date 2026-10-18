package vn.edu.uit.nearby.nearby_backend.identity.domain.password;

import vn.edu.uit.nearby.nearby_backend.identity.domain.EmailAddress;

public record PasswordCandidate(String password, EmailAddress owner) {

	public PasswordCandidate {
		password = password == null ? "" : password;
	}

}
