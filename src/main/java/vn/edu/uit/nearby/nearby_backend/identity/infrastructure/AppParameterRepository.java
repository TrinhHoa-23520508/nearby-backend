package vn.edu.uit.nearby.nearby_backend.identity.infrastructure;

import org.springframework.data.jpa.repository.JpaRepository;

import vn.edu.uit.nearby.nearby_backend.identity.domain.AppParameter;

public interface AppParameterRepository extends JpaRepository<AppParameter, String> {

}
