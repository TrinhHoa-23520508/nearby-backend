package vn.edu.uit.nearby.nearby_backend.identity.web;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import vn.edu.uit.nearby.nearby_backend.identity.application.account.PasswordResetService;
import vn.edu.uit.nearby.nearby_backend.identity.application.account.RegisterCommand;
import vn.edu.uit.nearby.nearby_backend.identity.application.account.RegistrationService;
import vn.edu.uit.nearby.nearby_backend.identity.application.auth.AuthService;
import vn.edu.uit.nearby.nearby_backend.identity.application.auth.LoginCommand;
import vn.edu.uit.nearby.nearby_backend.identity.web.dto.ForgotPasswordRequest;
import vn.edu.uit.nearby.nearby_backend.identity.web.dto.LoginRequest;
import vn.edu.uit.nearby.nearby_backend.identity.web.dto.LogoutRequest;
import vn.edu.uit.nearby.nearby_backend.identity.web.dto.RefreshTokenRequest;
import vn.edu.uit.nearby.nearby_backend.identity.web.dto.RegisterRequest;
import vn.edu.uit.nearby.nearby_backend.identity.web.dto.RegistrationResponse;
import vn.edu.uit.nearby.nearby_backend.identity.web.dto.ResendVerificationRequest;
import vn.edu.uit.nearby.nearby_backend.identity.web.dto.ResetPasswordRequest;
import vn.edu.uit.nearby.nearby_backend.identity.web.dto.TokenResponse;
import vn.edu.uit.nearby.nearby_backend.identity.web.dto.VerifyEmailRequest;
import vn.edu.uit.nearby.nearby_backend.shared.constant.ApiPaths;

@Tag(name = "Authentication", description = "Registration, email verification, login, tokens and password reset")
@SecurityRequirements
@RestController
@RequestMapping(ApiPaths.AUTH_BASE)
public class AuthController {

	private final RegistrationService registrationService;

	private final AuthService authService;

	private final PasswordResetService passwordResetService;

	public AuthController(RegistrationService registrationService, AuthService authService,
			PasswordResetService passwordResetService) {
		this.registrationService = registrationService;
		this.authService = authService;
		this.passwordResetService = passwordResetService;
	}

	@Operation(summary = "Register with email and password (FR-ID-01)")
	@PostMapping("/register")
	@ResponseStatus(HttpStatus.CREATED)
	public RegistrationResponse register(@Valid @RequestBody RegisterRequest request, HttpServletRequest http) {
		return RegistrationResponse.from(registrationService.register(
				new RegisterCommand(request.email(), request.password(), request.displayName(), http.getRemoteAddr())));
	}

	@Operation(summary = "Verify the email address with the token from the verification link")
	@PostMapping("/email-verification")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void verifyEmail(@Valid @RequestBody VerifyEmailRequest request) {
		registrationService.verifyEmail(request.token());
	}

	@Operation(summary = "Send a new verification email; same response whether or not the email exists")
	@PostMapping("/email-verification/resend")
	@ResponseStatus(HttpStatus.ACCEPTED)
	public void resendVerification(@Valid @RequestBody ResendVerificationRequest request) {
		registrationService.resendVerification(request.email());
	}

	@Operation(summary = "Log in and receive an access token and a refresh token (FR-ID-02)")
	@PostMapping("/login")
	public TokenResponse login(@Valid @RequestBody LoginRequest request) {
		return TokenResponse.from(authService.login(new LoginCommand(request.email(), request.password())));
	}

	@Operation(summary = "Exchange a refresh token for a new token pair; the old refresh token is revoked")
	@PostMapping("/refresh")
	public TokenResponse refresh(@Valid @RequestBody RefreshTokenRequest request) {
		return TokenResponse.from(authService.refresh(request.refreshToken()));
	}

	@Operation(summary = "Log out by revoking the refresh token (FR-ID-07)")
	@PostMapping("/logout")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void logout(@Valid @RequestBody LogoutRequest request) {
		authService.logout(request.refreshToken());
	}

	@Operation(summary = "Email a password reset link; same response whether or not the email exists (FR-ID-08)")
	@PostMapping("/password/forgot")
	@ResponseStatus(HttpStatus.ACCEPTED)
	public void forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
		passwordResetService.requestReset(request.email());
	}

	@Operation(summary = "Set a new password with the token from the reset link; logs out every session")
	@PostMapping("/password/reset")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
		passwordResetService.resetPassword(request.token(), request.newPassword());
	}

}
