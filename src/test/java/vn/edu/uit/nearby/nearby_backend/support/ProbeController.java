package vn.edu.uit.nearby.nearby_backend.support;

import java.util.Map;

import org.springframework.boot.test.context.TestComponent;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import vn.edu.uit.nearby.nearby_backend.shared.error.ConflictException;
import vn.edu.uit.nearby.nearby_backend.shared.error.InvalidRequestException;
import vn.edu.uit.nearby.nearby_backend.shared.error.OperationNotAllowedException;
import vn.edu.uit.nearby.nearby_backend.shared.error.RateLimitExceededException;
import vn.edu.uit.nearby.nearby_backend.shared.error.ResourceNotFoundException;
import vn.edu.uit.nearby.nearby_backend.shared.web.idempotency.Idempotent;

@TestComponent
@RestController
@RequestMapping("/api/v1/probe")
public class ProbeController {

	@GetMapping("/errors/{type}")
	void throwBusinessError(@PathVariable String type) {
		throw switch (type) {
			case "not-found" -> new ResourceNotFoundException("LISTING_NOT_FOUND", "Listing not found.");
			case "conflict" -> new ConflictException("OFFER_ALREADY_OPEN", "An offer is already open.");
			case "invalid" -> new InvalidRequestException("INVALID_PRICE", "Price must be positive.");
			case "forbidden" -> new OperationNotAllowedException("NOT_LISTING_OWNER", "Not the owner.");
			case "rate-limited" -> new RateLimitExceededException();
			default -> new IllegalStateException("Unexpected failure");
		};
	}

	@PostMapping("/validated")
	Map<String, String> validated(@Valid @RequestBody ProbeRequest request) {
		return Map.of("name", request.name());
	}

	@Idempotent
	@PostMapping("/idempotent")
	Map<String, String> idempotent() {
		return Map.of("status", "created");
	}

	@Idempotent
	@PostMapping("/idempotent-failing")
	void idempotentFailing() {
		throw new IllegalStateException("Downstream failure");
	}

	record ProbeRequest(@NotBlank String name) {
	}

}
