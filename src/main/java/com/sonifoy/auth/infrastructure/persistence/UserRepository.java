package com.sonifoy.auth.infrastructure.persistence;

import com.sonifoy.auth.domain.model.User;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

@Repository
public interface UserRepository extends R2dbcRepository<User, Long> {
    Mono<User> findByEmail(String email);

    Mono<Boolean> existsByEmail(String email);
}
