package org.cts.fp_identity.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.cts.fp_identity.repository.BlacklistedTokenRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * Mirrors the monolith's ReportWorker.cleanExpiredTokens() exactly.
 * Runs at 00:30 every day and removes expired JWT tokens from the blacklist.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class TokenCleanupScheduler {

    private final BlacklistedTokenRepository blacklistedTokenRepository;

    @Scheduled(cron = "0 30 0 * * *")   // 00:30 every day
    public void cleanExpiredTokens() {
        blacklistedTokenRepository.deleteExpiredTokens(LocalDateTime.now());
        log.info("TokenCleanupScheduler: expired blacklisted tokens cleaned up");
    }
}
