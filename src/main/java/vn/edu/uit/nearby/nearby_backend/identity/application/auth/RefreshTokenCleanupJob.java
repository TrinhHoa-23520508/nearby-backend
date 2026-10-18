package vn.edu.uit.nearby.nearby_backend.identity.application.auth;

import java.time.Clock;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import lombok.extern.slf4j.Slf4j;

import vn.edu.uit.nearby.nearby_backend.identity.infrastructure.RefreshTokenRepository;

/**
 * Deletes expired refresh tokens (SRS 4.4). Revoked tokens are kept until they expire so
 * that reusing one is still detected.
 */
@Slf4j
@Component
public class RefreshTokenCleanupJob {

	private static final String DAILY_AT_03_30 = "0 30 3 * * *";

	private final RefreshTokenRepository refreshTokens;

	private final Clock clock;

	public RefreshTokenCleanupJob(RefreshTokenRepository refreshTokens, Clock clock) {
		this.refreshTokens = refreshTokens;
		this.clock = clock;
	}

	@Transactional
	@Scheduled(cron = DAILY_AT_03_30, zone = "UTC")
	public void deleteExpiredTokens() {
		int deleted = refreshTokens.deleteExpiredBefore(clock.instant());
		log.info("Deleted {} expired refresh tokens", deleted);
	}

}
