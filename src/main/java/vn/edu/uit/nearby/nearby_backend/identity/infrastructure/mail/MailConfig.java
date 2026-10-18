package vn.edu.uit.nearby.nearby_backend.identity.infrastructure.mail;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.javamail.JavaMailSender;

import lombok.extern.slf4j.Slf4j;

import vn.edu.uit.nearby.nearby_backend.identity.application.IdentityProperties;
import vn.edu.uit.nearby.nearby_backend.identity.application.port.AccountMailer;

@Slf4j
@Configuration(proxyBeanMethods = false)
public class MailConfig {

	@Bean
	AccountMailer accountMailer(IdentityProperties properties, ObjectProvider<JavaMailSender> mailSender) {
		IdentityProperties.Mail mail = properties.mail();
		if (mail.enabled()) {
			return new SmtpAccountMailer(mailSender.getObject(), mail.from());
		}
		log.warn("Mail is disabled (app.identity.mail.enabled=false): account emails are written to the log");
		return new LoggingAccountMailer();
	}

}
