package vn.edu.uit.nearby.nearby_backend.identity.application.admin;

import java.time.Clock;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import lombok.extern.slf4j.Slf4j;

import vn.edu.uit.nearby.nearby_backend.identity.application.IdentityProperties;
import vn.edu.uit.nearby.nearby_backend.identity.application.IdentityProperties.BootstrapAdmin;
import vn.edu.uit.nearby.nearby_backend.identity.domain.EmailAddress;
import vn.edu.uit.nearby.nearby_backend.identity.domain.User;
import vn.edu.uit.nearby.nearby_backend.identity.domain.password.PasswordPolicy;
import vn.edu.uit.nearby.nearby_backend.identity.infrastructure.UserRepository;

/**
 * Admin accounts cannot be registered through the API, so the first one is created from
 * configuration ({@code app.identity.bootstrap-admin}).
 */
@Slf4j
@Component
public class AdminAccountInitializer {

	private final UserRepository users;

	private final PasswordPolicy passwordPolicy;

	private final PasswordEncoder passwordEncoder;

	private final IdentityProperties properties;

	private final Clock clock;

	public AdminAccountInitializer(UserRepository users, PasswordPolicy passwordPolicy, PasswordEncoder passwordEncoder,
			IdentityProperties properties, Clock clock) {
		this.users = users;
		this.passwordPolicy = passwordPolicy;
		this.passwordEncoder = passwordEncoder;
		this.properties = properties;
		this.clock = clock;
	}

	@Transactional
	@EventListener(ApplicationReadyEvent.class)
	public void createBootstrapAdmin() {
		BootstrapAdmin admin = properties.bootstrapAdmin();
		if (admin == null || !admin.isConfigured()) {
			return;
		}
		EmailAddress email = EmailAddress.of(admin.email());
		if (users.existsByEmail(email.value())) {
			return;
		}
		passwordPolicy.validate(admin.password(), email);
		users.save(User.administrator(email, passwordEncoder.encode(admin.password()), admin.displayName(),
				clock.instant()));
		log.info("Created bootstrap admin account {}", email.value());
	}

}
