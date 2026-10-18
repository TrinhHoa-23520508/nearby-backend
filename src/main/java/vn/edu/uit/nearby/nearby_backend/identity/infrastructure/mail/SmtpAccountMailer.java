package vn.edu.uit.nearby.nearby_backend.identity.infrastructure.mail;

import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;

import lombok.extern.slf4j.Slf4j;

import vn.edu.uit.nearby.nearby_backend.identity.application.port.AccountMailer;
import vn.edu.uit.nearby.nearby_backend.identity.domain.EmailAddress;

@Slf4j
public class SmtpAccountMailer implements AccountMailer {

	private final JavaMailSender mailSender;

	private final String sender;

	public SmtpAccountMailer(JavaMailSender mailSender, String sender) {
		this.mailSender = mailSender;
		this.sender = sender;
	}

	@Async
	@Override
	public void sendEmailVerification(EmailAddress recipient, String displayName, String verificationLink) {
		send(recipient, AccountEmailTemplates.VERIFICATION_SUBJECT,
				AccountEmailTemplates.verificationBody(displayName, verificationLink));
	}

	@Async
	@Override
	public void sendPasswordReset(EmailAddress recipient, String displayName, String resetLink) {
		send(recipient, AccountEmailTemplates.PASSWORD_RESET_SUBJECT,
				AccountEmailTemplates.passwordResetBody(displayName, resetLink));
	}

	// The user can request the email again, so a delivery failure is logged, not
	// rethrown.
	private void send(EmailAddress recipient, String subject, String body) {
		SimpleMailMessage message = new SimpleMailMessage();
		message.setFrom(sender);
		message.setTo(recipient.value());
		message.setSubject(subject);
		message.setText(body);
		try {
			mailSender.send(message);
		}
		catch (MailException ex) {
			log.error("Failed to send '{}' email: {}", subject, ex.getMessage());
		}
	}

}
