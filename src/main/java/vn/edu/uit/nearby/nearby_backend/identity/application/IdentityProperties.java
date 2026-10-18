package vn.edu.uit.nearby.nearby_backend.identity.application;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("app.identity")
public record IdentityProperties(String emailVerificationUrl, String passwordResetUrl, Mail mail,
		BootstrapAdmin bootstrapAdmin) {

	public record Mail(boolean enabled, String from) {
	}

	public record BootstrapAdmin(String email, String password, String displayName) {

		public boolean isConfigured() {
			return email != null && !email.isBlank() && password != null && !password.isBlank();
		}

	}

}
