package com.sonifoy.auth.application.service;

import com.sonifoy.auth.adapter.web.dto.AuthResponse;
import com.sonifoy.auth.domain.model.RefreshToken;
import com.sonifoy.auth.domain.model.User;
import com.sonifoy.auth.infrastructure.persistence.RefreshTokenRepository;
import com.sonifoy.auth.infrastructure.persistence.UserRepository;
import com.sonifoy.auth.infrastructure.security.JwtService;
import com.sonifoy.auth.infrastructure.security.RedisSessionKeyStore;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final RedisSessionKeyStore redisSessionKeyStore;

    private static final SecureRandom secureRandom = new SecureRandom();

    public Mono<User> register(User user) {
        return userRepository.existsByEmail(user.getEmail())
                .flatMap(exists -> {
                    if (exists) {
                        return Mono.error(new RuntimeException("Email already in use"));
                    }
                    user.setPassword(passwordEncoder.encode(user.getPassword()));
                    if (user.getProfileType() == null) {
                        user.setProfileType("LISTENER"); // Default
                    }
                    user.setCreatedAt(LocalDateTime.now());
                    user.setUpdatedAt(LocalDateTime.now());

                    // Initial verification code
                    String verificationCode = String.format("%06d", secureRandom.nextInt(1000000));
                    user.setVerificationCode(verificationCode);
                    log.info("Generated verification code for {}: {}", user.getEmail(), verificationCode);

                    return userRepository.save(user)
                            .doOnSuccess(u -> log.info("Registered new user: {}", u.getEmail()));
                });
    }

    public Mono<User> verifyEmail(String email, String code) {
        return userRepository.findByEmail(email)
                .switchIfEmpty(Mono.error(new RuntimeException("User not found")))
                .flatMap(user -> {
                    if (user.getVerificationCode() != null && user.getVerificationCode().equals(code)) {
                        user.setVerified(true);
                        user.setVerificationCode(null);
                        user.setUpdatedAt(LocalDateTime.now());
                        return userRepository.save(user);
                    }
                    return Mono.error(new RuntimeException("Invalid verification code"));
                });
    }

    public Mono<Void> resendVerificationCode(String email) {
        return userRepository.findByEmail(email)
                .switchIfEmpty(Mono.error(new RuntimeException("User not found")))
                .flatMap(user -> {
                    String newCode = String.format("%06d", secureRandom.nextInt(1000000));
                    user.setVerificationCode(newCode);
                    user.setUpdatedAt(LocalDateTime.now());
                    return userRepository.save(user);
                })
                .then();
    }

    public Mono<AuthResponse> login(String email, String password) {
        return userRepository.findByEmail(email)
                .switchIfEmpty(Mono.error(new RuntimeException("User not found")))
                .flatMap(user -> {
                    if (!passwordEncoder.matches(password, user.getPassword())) {
                        return Mono.error(new RuntimeException("Invalid credentials"));
                    }

                    // 1. Generate Access Token
                    String accessToken = jwtService.generateToken(user.getEmail());

                    // 2. Create Refresh Token (Persistent)
                    String refreshTokenStr = UUID.randomUUID().toString();
                    RefreshToken refreshTokenEntity = RefreshToken.builder()
                            .token(refreshTokenStr)
                            .userId(user.getId())
                            .expiresAt(LocalDateTime.now().plusDays(7)) // 7 Days
                            .createdAt(LocalDateTime.now())
                            .isRevoked(false)
                            .build();

                    // 3. Create Session (Redis) for Encryption
                    String sessionId = UUID.randomUUID().toString();
                    byte[] sessionKey = new byte[32]; // 256-bit key
                    secureRandom.nextBytes(sessionKey);

                    Mono<Void> sessionSaveMono = redisSessionKeyStore.saveKey(sessionId, sessionKey);
                    Mono<RefreshToken> refreshTokenSaveMono = refreshTokenRepository.save(refreshTokenEntity);

                    return Mono.when(sessionSaveMono, refreshTokenSaveMono)
                            .then(Mono.just(AuthResponse.builder()
                                    .accessToken(accessToken)
                                    .refreshToken(refreshTokenStr)
                                    .sessionId(sessionId)
                                    .user(user)
                                    .build()));
                });
    }

    public Mono<Void> logout(String sessionId) {
        // Remove session key from Redis
        return redisSessionKeyStore.removeKey(sessionId);
    }

    public Mono<AuthResponse> refreshToken(String requestRefreshToken) {
        return refreshTokenRepository.findByToken(requestRefreshToken)
                .switchIfEmpty(Mono.error(new RuntimeException("Refresh token not found")))
                .flatMap(refreshToken -> {
                    if (refreshToken.isRevoked() || refreshToken.getExpiresAt().isBefore(LocalDateTime.now())) {
                        return Mono.error(new RuntimeException("Invalid or expired refresh token"));
                    }

                    return userRepository.findById(refreshToken.getUserId())
                            .switchIfEmpty(Mono.error(new RuntimeException("User not found")))
                            .flatMap(user -> {
                                // Revoke old token
                                refreshToken.setRevoked(true);

                                // Generate new Access Token
                                String newAccessToken = jwtService.generateToken(user.getEmail());

                                // Generate new Refresh Token
                                String newRefreshTokenStr = UUID.randomUUID().toString();
                                RefreshToken newRefreshTokenEntity = RefreshToken.builder()
                                        .token(newRefreshTokenStr)
                                        .userId(user.getId())
                                        .expiresAt(LocalDateTime.now().plusDays(7))
                                        .createdAt(LocalDateTime.now())
                                        .isRevoked(false)
                                        .build();

                                // Reuse or rotate session?
                                // Usually we keep the session ID unless we want to force re-encryption
                                // handshake.
                                // For simplicity, we just return the tokens. Session ID remains valid in Redis
                                // until TTL.
                                // If client lost Session ID, they must login again.

                                return refreshTokenRepository.save(refreshToken) // Save revoked
                                        .then(refreshTokenRepository.save(newRefreshTokenEntity)) // Save new
                                        .map(saved -> AuthResponse.builder()
                                                .accessToken(newAccessToken)
                                                .refreshToken(newRefreshTokenStr)
                                                // .sessionId? Ideally client keeps it. If we don't return it, client
                                                // uses old one.
                                                .user(user)
                                                .build());
                            });
                });
    }
}
