package vn.edu.uit.nearby.nearby_backend.support;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.boot.test.context.TestComponent;
import org.springframework.context.annotation.Primary;
import org.springframework.web.util.UriComponentsBuilder;

import vn.edu.uit.nearby.nearby_backend.identity.application.port.AccountMailer;
import vn.edu.uit.nearby.nearby_backend.identity.domain.EmailAddress;

/**
 * Replaces SMTP in integration tests and keeps the last token sent to each address.
 */
@Primary
@TestComponent
public class RecordingAccountMailer implements AccountMailer {

	private final Map<String, String> verificationTokens = new ConcurrentHashMap<>();

	private final Map<String, String> passwordResetTokens = new ConcurrentHashMap<>();

	@Override
	public void sendEmailVerification(EmailAddress recipient, String displayName, String verificationLink) {
		verificationTokens.put(recipient.value(), tokenOf(verificationLink));
	}

	@Override
	public void sendPasswordReset(EmailAddress recipient, String displayName, String resetLink) {
		passwordResetTokens.put(recipient.value(), tokenOf(resetLink));
	}

	public Optional<String> verificationTokenFor(String email) {
		return Optional.ofNullable(verificationTokens.get(email));
	}

	public Optional<String> passwordResetTokenFor(String email) {
		return Optional.ofNullable(passwordResetTokens.get(email));
	}

	private static String tokenOf(String link) {
		return UriComponentsBuilder.fromUriString(link).build().getQueryParams().getFirst("token");
	}

}
