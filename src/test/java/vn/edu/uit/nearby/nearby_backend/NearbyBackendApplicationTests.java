package vn.edu.uit.nearby.nearby_backend;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import vn.edu.uit.nearby.nearby_backend.support.IntegrationTest;

@IntegrationTest
class NearbyBackendApplicationTests {

	private static final int DATA_DESIGN_TABLES = 22;

	private static final int OUTBOX_TABLES = 1;

	@Autowired
	JdbcTemplate jdbc;

	@Test
	void migrationsCreateEveryTableOfTheDataDesignPlusTheOutbox() {
		Integer tableCount = jdbc.queryForObject("""
				SELECT count(*) FROM information_schema.tables
				WHERE table_schema = 'public' AND table_type = 'BASE TABLE'
				  AND table_name NOT IN ('flyway_schema_history', 'spatial_ref_sys')
				""", Integer.class);

		assertThat(tableCount).isEqualTo(DATA_DESIGN_TABLES + OUTBOX_TABLES);
	}

	@Test
	void postgisExtensionIsInstalled() {
		assertThat(jdbc.queryForObject("SELECT postgis_version()", String.class)).isNotBlank();
	}

	@Test
	void openOfferIsUniquePerListingAndBuyer() {
		Integer uniqueIndexes = jdbc.queryForObject("""
				SELECT count(*) FROM pg_indexes
				WHERE indexname = 'transactions_open_offer_uq' AND indexdef LIKE '%WHERE%'
				""", Integer.class);

		assertThat(uniqueIndexes).isOne();
	}

}
