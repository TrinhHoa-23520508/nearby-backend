package vn.edu.uit.nearby.nearby_backend.identity.application.port;

import vn.edu.uit.nearby.nearby_backend.identity.domain.EmailAddress;

/**
 * Sends account emails. Implementations send asynchronously and never fail the caller.
 */
public interface AccountMailer {

	void sendEmailVerification(EmailAddress recipient, String displayName, String verificationLink);

	void sendPasswordReset(EmailAddress recipient, String displayName, String resetLink);

}
