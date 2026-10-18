package vn.edu.uit.nearby.nearby_backend.identity.application;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import vn.edu.uit.nearby.nearby_backend.identity.AppParameters;
import vn.edu.uit.nearby.nearby_backend.identity.domain.AppParameter;
import vn.edu.uit.nearby.nearby_backend.identity.infrastructure.AppParameterRepository;

@Service
class AppParameterService implements AppParameters {

	private final AppParameterRepository repository;

	AppParameterService(AppParameterRepository repository) {
		this.repository = repository;
	}

	@Override
	@Transactional(readOnly = true)
	public String get(String key) {
		return repository.findById(key)
			.map(AppParameter::getValue)
			.orElseThrow(() -> new IllegalStateException("Missing app parameter: " + key));
	}

}
