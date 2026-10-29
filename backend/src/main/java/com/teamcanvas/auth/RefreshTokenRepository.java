package com.teamcanvas.auth;

import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    /**
     * Locks the row (SELECT ... FOR UPDATE) until the surrounding transaction ends. If the same refresh token is
     * submitted twice at the same instant, the second request waits, then sees it as revoked and gets 401.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<RefreshToken> findByTokenHash(String tokenHash);
}
