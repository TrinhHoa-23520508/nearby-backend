package vn.edu.uit.nearby.nearby_backend.identity.domain;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "app_parameters")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AppParameter {

	@Id
	@Column(name = "key", length = 60)
	private String key;

	@Column(nullable = false, length = 200)
	private String value;

	@Column(length = 200)
	private String description;

	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

}
