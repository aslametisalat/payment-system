package com.payment.common.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;

/**
 * Issues and validates the demo bearer tokens every service in the flow
 * requires (see JwtAuthenticationFilter). security-service is the only
 * service that calls generateToken() (from its AuthController) - every
 * service, including security-service itself, calls validateAndGetSubject()
 * to check an incoming request's token.
 *
 * The signing secret is a DEMO value from config (payment.security.jwt-
 * secret, same one in every service's application.yml - a real deployment
 * would pull this from a secrets manager, not a YAML file), and there's no
 * real user store behind token issuance - see AuthController. This gets
 * the actual mechanics right (a real HS256-signed, expiring JWT that every
 * service independently verifies) without pretending this is a production
 * identity provider.
 */
@Component
public class JwtUtil {

    @Value("${payment.security.jwt-secret}")
    private String secret;

    @Value("${payment.security.jwt-expiration-ms:3600000}")
    private long expirationMs;

    private SecretKey key() {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    public String generateToken(String subject) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(subject)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusMillis(expirationMs)))
                .signWith(key())
                .compact();
    }

    /**
     * @return the token's subject if it's validly signed and unexpired
     * @throws JwtException if the token is missing, malformed, expired, or signed with a different key
     */
    public String validateAndGetSubject(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(key())
                .build()
                .parseSignedClaims(token)
                .getPayload();
        return claims.getSubject();
    }
}
