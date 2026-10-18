package vn.edu.uit.nearby.nearby_backend.identity.infrastructure.mail;

import lombok.extern.slf4j.Slf4j;

import vn.edu.uit.nearby.nearby_backend.identity.application.port.AccountMailer;
import vn.edu.uit.nearby.nearby_backend.identity.domain.EmailAddress;

/**
 * Local development only: the links contain live tokens, so never enable this in
 * production.
 */
@Slf4j
public class LoggingAccountMailer implements AccountMailer {

	@Override
	public void sendEmailVerification(EmailAddress recipient, String displayName, String verificationLink) {
		log.info("[mail disabled] Email verification for {}: {}", recipient.value(), verificationLink);
	}

	@Override
	public void sendPasswordReset(EmailAddress recipient, String displayName, String resetLink) {
		log.info("[mail disabled] Password reset for {}: {}", recipient.value(), resetLink);
	}

}
