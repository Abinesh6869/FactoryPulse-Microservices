package org.cts.fp_identity.repository;

import org.cts.fp_identity.model.BlacklistedToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

public interface BlacklistedTokenRepository extends JpaRepository<BlacklistedToken, Long> {
    boolean existsByToken(String token);

    @Modifying
    @Transactional
    @Query("DELETE FROM BlacklistedToken b WHERE b.expiresAt < :now")
    void deleteExpiredTokens(LocalDateTime now);
}
