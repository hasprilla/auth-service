package com.sonifoy.auth.infrastructure.persistence;

import com.sonifoy.auth.domain.model.RefreshToken;
import org.springframework.data.r2dbc.repository.Modifying;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import reactor.core.publisher.Mono;

public interface RefreshTokenRepository extends R2dbcRepository<RefreshToken, Long> {
    Mono<RefreshToken> findByToken(String token);

    @Modifying
    @Query("UPDATE refresh_tokens SET revoked = true WHERE user_id = :userId")
    Mono<Void> revokeAllByUserId(String userId);
}
