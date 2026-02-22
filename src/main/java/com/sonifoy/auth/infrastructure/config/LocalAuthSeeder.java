package com.sonifoy.auth.infrastructure.config;

import com.github.javafaker.Faker;
import com.sonifoy.auth.domain.model.User;
import com.sonifoy.auth.infrastructure.persistence.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

@Component
@Profile("local")
@RequiredArgsConstructor
@Slf4j
public class LocalAuthSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final Faker faker = new Faker();

    private static final int USER_COUNT = 20000;
    private static final int BATCH_SIZE = 500;
    private static final String DEFAULT_PASSWORD = "Perros123*";

    @Override
    public void run(String... args) {
        log.info("LocalAuthSeeder execution started.");
        userRepository.count()
                .flatMap(count -> {
                    if (count >= USER_COUNT) {
                        log.info("Auth database already seeded with {} users.", count);
                        return Mono.empty();
                    }
                    log.info("Seeding {} auth records (this may take a while)...", USER_COUNT);
                    return seedInitialData();
                })
                .subscribe();
    }

    private Mono<Void> seedInitialData() {
        final String encodedPassword = passwordEncoder.encode(DEFAULT_PASSWORD);

        // 1. Create Admin
        User admin = User.builder()
                .email("harveyasprilla@gmail.com")
                .name("Harvey Asprilla")
                .password(encodedPassword)
                .roles(new HashSet<>(Set.of("ADMIN", "USER")))
                .verified(true)
                .profileType("PREMIUM")
                .build();

        return userRepository.findByEmail(admin.getEmail())
                .switchIfEmpty(userRepository.save(admin))
                .then(Mono.defer(() -> {
                    AtomicInteger counter = new AtomicInteger(0);
                    return Flux.range(1, USER_COUNT)
                            .buffer(BATCH_SIZE)
                            .flatMap(batch -> {
                                return userRepository.saveAll(
                                        batch.stream().map(i -> {
                                            String email = "user" + i + "@example.com";
                                            return User.builder()
                                                    .email(email)
                                                    .name(faker.name().fullName())
                                                    .password(encodedPassword)
                                                    .roles(new HashSet<>(Collections.singletonList("USER")))
                                                    .verified(true)
                                                    .profileType("LISTENER")
                                                    .build();
                                        }).toList()).collectList()
                                        .doOnNext(list -> log.info("Auth Seeding progress: {}/20000",
                                                counter.addAndGet(list.size())));
                            })
                            .then();
                }))
                .doOnSuccess(v -> log.info("Auth seeding completed successfully."));
    }
}
