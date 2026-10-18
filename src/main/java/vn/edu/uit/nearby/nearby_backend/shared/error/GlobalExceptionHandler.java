package vn.edu.uit.nearby.nearby_backend.shared.error;

import java.net.URI;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.jspecify.annotations.Nullable;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;

import vn.edu.uit.nearby.nearby_backend.shared.constant.ErrorCodes;
import vn.edu.uit.nearby.nearby_backend.shared.constant.SecurityConstants;
import vn.edu.uit.nearby.nearby_backend.shared.security.CurrentUser;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

	@ExceptionHandler(BusinessException.class)
	ResponseEntity<ProblemDetail> handleBusiness(BusinessException ex) {
		HttpStatus status = statusOf(ex);
		ProblemDetail problem = problem(status, ex.getCode(), ex.getMessage());
		ex.getProperties().forEach(problem::setProperty);
		ResponseEntity.BodyBuilder response = ResponseEntity.status(status);
		if (ex instanceof RateLimitExceededException rateLimited) {
			rateLimited.getRetryAfter()
				.ifPresent(retryAfter -> response.header(HttpHeaders.RETRY_AFTER, retryAfterSeconds(retryAfter)));
		}
		return response.body(problem);
	}

	@ExceptionHandler(AuthenticationException.class)
	ResponseEntity<ProblemDetail> handleAuthentication(AuthenticationException ex) {
		return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
			.header(HttpHeaders.WWW_AUTHENTICATE, SecurityConstants.BEARER_SCHEME)
			.body(problem(HttpStatus.UNAUTHORIZED, ErrorCodes.UNAUTHENTICATED, "A valid access token is required."));
	}

	@ExceptionHandler(AccessDeniedException.class)
	ProblemDetail handleAccessDenied(AccessDeniedException ex, HttpServletRequest request) {
		log.warn("Access denied for user {} on {} {}", CurrentUser.id().map(UUID::toString).orElse("anonymous"),
				request.getMethod(), request.getRequestURI());
		return problem(HttpStatus.FORBIDDEN, ErrorCodes.ACCESS_DENIED, "You are not allowed to perform this action.");
	}

	@ExceptionHandler(OptimisticLockingFailureException.class)
	ProblemDetail handleOptimisticLock(OptimisticLockingFailureException ex) {
		return problem(HttpStatus.CONFLICT, ErrorCodes.CONCURRENT_MODIFICATION,
				"The resource was modified by another request; reload and retry.");
	}

	@ExceptionHandler(DataIntegrityViolationException.class)
	ProblemDetail handleDataIntegrity(DataIntegrityViolationException ex) {
		log.debug("Data integrity violation", ex);
		return problem(HttpStatus.CONFLICT, ErrorCodes.DATA_CONFLICT, "The request conflicts with existing data.");
	}

	@ExceptionHandler(Exception.class)
	ProblemDetail handleUnexpected(Exception ex) {
		log.error("Unhandled exception", ex);
		return problem(HttpStatus.INTERNAL_SERVER_ERROR, ErrorCodes.INTERNAL_ERROR, "An unexpected error occurred.");
	}

	@Override
	protected @Nullable ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
			HttpHeaders headers, HttpStatusCode status, WebRequest request) {
		ProblemDetail problem = problem(HttpStatus.BAD_REQUEST, ErrorCodes.VALIDATION_FAILED,
				"Request validation failed.");
		problem.setProperty("errors", fieldErrorsOf(ex));
		return handleExceptionInternal(ex, problem, headers, HttpStatus.BAD_REQUEST, request);
	}

	private static HttpStatus statusOf(BusinessException ex) {
		return switch (ex) {
			case ResourceNotFoundException e -> HttpStatus.NOT_FOUND;
			case ConflictException e -> HttpStatus.CONFLICT;
			case InvalidRequestException e -> HttpStatus.BAD_REQUEST;
			case OperationNotAllowedException e -> HttpStatus.FORBIDDEN;
			case RateLimitExceededException e -> HttpStatus.TOO_MANY_REQUESTS;
			case AuthenticationFailedException e -> HttpStatus.UNAUTHORIZED;
		};
	}

	private static String retryAfterSeconds(Duration retryAfter) {
		return String.valueOf(Math.max(1, retryAfter.toSeconds()));
	}

	private static List<Map<String, String>> fieldErrorsOf(MethodArgumentNotValidException ex) {
		return ex.getBindingResult()
			.getFieldErrors()
			.stream()
			.map(error -> Map.of("field", error.getField(), "message", String.valueOf(error.getDefaultMessage())))
			.toList();
	}

	private static ProblemDetail problem(HttpStatus status, String code, String detail) {
		ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
		problem.setType(URI.create(ErrorCodes.PROBLEM_TYPE_BASE + code.toLowerCase().replace('_', '-')));
		problem.setProperty("code", code);
		return problem;
	}

}
