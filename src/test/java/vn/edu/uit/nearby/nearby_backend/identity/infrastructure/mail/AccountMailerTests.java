package vn.edu.uit.nearby.nearby_backend.identity.infrastructure.mail;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import vn.edu.uit.nearby.nearby_backend.identity.application.IdentityProperties;
import vn.edu.uit.nearby.nearby_backend.identity.domain.EmailAddress;

@ExtendWith(MockitoExtension.class)
class AccountMailerTests {

	private static final EmailAddress RECIPIENT = EmailAddress.of("an@example.com");

	private static final String SENDER = "no-reply@nearby.vn";

	@Mock
	JavaMailSender mailSender;

	@Mock
	ObjectProvider<JavaMailSender> mailSenderProvider;

	@Test
	void verificationEmailContainsTheLink() {
		new SmtpAccountMailer(mailSender, SENDER).sendEmailVerification(RECIPIENT, "An", "nearby://verify?token=t1");

		SimpleMailMessage message = sentMessage();
		assertThat(message.getTo()).containsExactly("an@example.com");
		assertThat(message.getFrom()).isEqualTo(SENDER);
		assertThat(message.getSubject()).isEqualTo(AccountEmailTemplates.VERIFICATION_SUBJECT);
		assertThat(message.getText()).contains("An", "nearby://verify?token=t1");
	}

	@Test
	void passwordResetEmailContainsTheLink() {
		new SmtpAccountMailer(mailSender, SENDER).sendPasswordReset(RECIPIENT, "An", "nearby://reset?token=t2");

		SimpleMailMessage message = sentMessage();
		assertThat(message.getSubject()).isEqualTo(AccountEmailTemplates.PASSWORD_RESET_SUBJECT);
		assertThat(message.getText()).contains("nearby://reset?token=t2");
	}

	@Test
	void deliveryFailureDoesNotPropagate() {
		doThrow(new MailSendException("SMTP down")).when(mailSender).send(any(SimpleMailMessage.class));

		assertThatCode(() -> new SmtpAccountMailer(mailSender, SENDER).sendEmailVerification(RECIPIENT, "An", "link"))
			.doesNotThrowAnyException();
	}

	@Test
	void loggingMailerNeverFails() {
		LoggingAccountMailer mailer = new LoggingAccountMailer();

		assertThatCode(() -> {
			mailer.sendEmailVerification(RECIPIENT, "An", "link");
			mailer.sendPasswordReset(RECIPIENT, "An", "link");
		}).doesNotThrowAnyException();
	}

	@Test
	void smtpIsUsedOnlyWhenMailIsEnabled() {
		when(mailSenderProvider.getObject()).thenReturn(mailSender);

		assertThat(new MailConfig().accountMailer(propertiesWithMail(true), mailSenderProvider))
			.isInstanceOf(SmtpAccountMailer.class);
		assertThat(new MailConfig().accountMailer(propertiesWithMail(false), mailSenderProvider))
			.isInstanceOf(LoggingAccountMailer.class);
	}

	private SimpleMailMessage sentMessage() {
		ArgumentCaptor<SimpleMailMessage> message = ArgumentCaptor.forClass(SimpleMailMessage.class);
		verify(mailSender).send(message.capture());
		return message.getValue();
	}

	private static IdentityProperties propertiesWithMail(boolean enabled) {
		return new IdentityProperties("nearby://verify-email", "nearby://reset-password",
				new IdentityProperties.Mail(enabled, SENDER), null);
	}

}
