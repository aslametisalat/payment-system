package com.payment.security.controller;

import com.payment.common.security.JwtUtil;
import com.payment.security.dto.TokenRequest;
import com.payment.security.dto.TokenResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Issues the bearer tokens JwtAuthenticationFilter requires everywhere
 * else in the flow. There's no real user store or password hashing behind
 * this - one hardcoded demo login, on purpose: this project's actual point
 * is showing what protecting a payment API with tokens looks like end to
 * end (issue -> attach -> validate -> relay through Feign -> reject when
 * missing/invalid), not building an identity provider. A real system
 * issues these from Okta/Auth0/Keycloak/its own IdP, with real users,
 * real password policies, and (usually) short-lived tokens plus refresh
 * tokens - swap this controller out for a client of one of those and
 * every other service's filter keeps working unchanged, since they only
 * ever see "a validly-signed token or not."
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private static final String DEMO_USERNAME = "demo";
    private static final String DEMO_PASSWORD = "demo123";

    private final JwtUtil jwtUtil;

    @Value("${payment.security.jwt-expiration-ms:3600000}")
    private long expirationMs;

    /**
     * Demo credentials only: username 'demo', password 'demo123'. Attach
     * the returned token as "Authorization: Bearer &lt;token&gt;" on every
     * other call.
     */
    @PostMapping("/token")
    public ResponseEntity<?> issueToken(@RequestBody TokenRequest request) {
        if (!DEMO_USERNAME.equals(request.getUsername()) || !DEMO_PASSWORD.equals(request.getPassword())) {
            log.warn("Rejected token request for username: {}", request.getUsername());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("{\"error\":\"Invalid credentials\"}");
        }
        String token = jwtUtil.generateToken(request.getUsername());
        log.info("Issued token for {}", request.getUsername());
        return ResponseEntity.ok(TokenResponse.builder()
                .accessToken(token)
                .tokenType("Bearer")
                .expiresInSeconds(expirationMs / 1000)
                .build());
    }

    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Security Service Operational");
    }
}
