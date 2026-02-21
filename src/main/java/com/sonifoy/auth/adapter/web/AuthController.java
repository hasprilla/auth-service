package com.sonifoy.auth.adapter.web;

import com.sonifoy.auth.adapter.web.dto.AuthResponse;
import com.sonifoy.auth.adapter.web.dto.RefreshTokenRequest;
import com.sonifoy.auth.adapter.web.dto.ResendVerifyRequest;
import com.sonifoy.auth.adapter.web.dto.VerifyRequest;
import com.sonifoy.auth.application.service.AuthService;
import com.sonifoy.auth.domain.model.User;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public Mono<ResponseEntity<User>> register(@RequestBody User user) {
        return authService.register(user)
                .map(ResponseEntity::ok);
    }

    @PostMapping("/login")
    public Mono<ResponseEntity<AuthResponse>> login(@RequestBody User loginRequest) {
        return authService.login(loginRequest.getEmail(), loginRequest.getPassword())
                .map(ResponseEntity::ok);
    }

    @PostMapping("/refresh")
    public Mono<ResponseEntity<AuthResponse>> refreshToken(@RequestBody RefreshTokenRequest request) {
        return authService.refreshToken(request.getRefreshToken())
                .map(ResponseEntity::ok);
    }

    @PostMapping("/verify")
    public Mono<ResponseEntity<User>> verify(@RequestBody VerifyRequest request) {
        return authService.verifyEmail(request.getEmail(), request.getCode())
                .map(ResponseEntity::ok);
    }

    @PostMapping("/resend-verify")
    public Mono<ResponseEntity<Void>> resendVerify(@RequestBody ResendVerifyRequest request) {
        return authService.resendVerificationCode(request.getEmail())
                .then(Mono.just(ResponseEntity.ok().build()));
    }

    @PostMapping("/logout")
    public Mono<ResponseEntity<Void>> logout(@RequestParam(required = false) String sessionId) {
        if (sessionId != null) {
            return authService.logout(sessionId)
                    .then(Mono.just(ResponseEntity.ok().build()));
        }
        return Mono.just(ResponseEntity.ok().build());
    }
}
