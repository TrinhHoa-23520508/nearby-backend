package vn.edu.uit.nearby.nearby_backend.shared.web;

import java.time.Instant;
import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import vn.edu.uit.nearby.nearby_backend.shared.constant.ApiPaths;

@RestController
@RequestMapping(ApiPaths.TEST_BASE)
public class TestController {

	@GetMapping
	public Map<String, Object> ping() {
		return Map.of("status", "ok", "message", "Nearby backend is running", "timestamp", Instant.now().toString());
	}

	@GetMapping("/hello")
	public Map<String, String> hello(@RequestParam(defaultValue = "world") String name) {
		return Map.of("message", "Hello, " + name + "!");
	}

	@PostMapping("/echo")
	public Map<String, Object> echo(@RequestBody Map<String, Object> body) {
		return Map.of("received", body, "timestamp", Instant.now().toString());
	}

}
