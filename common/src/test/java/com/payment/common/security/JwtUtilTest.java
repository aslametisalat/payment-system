package com.payment.common.security;

import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtUtilTest {

    private JwtUtil jwtUtil;

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil();
        ReflectionTestUtils.setField(jwtUtil, "secret", "test-only-hmac-signing-secret-not-a-real-secret-1234567890");
        ReflectionTestUtils.setField(jwtUtil, "expirationMs", 3600000L);
    }

    @Test
    void generateToken_roundTripsTheSubject() {
        String token = jwtUtil.generateToken("demo");

        assertThat(jwtUtil.validateAndGetSubject(token)).isEqualTo("demo");
    }

    @Test
    void validateAndGetSubject_rejectsATokenSignedWithADifferentSecret() {
        JwtUtil otherIssuer = new JwtUtil();
        ReflectionTestUtils.setField(otherIssuer, "secret", "a-completely-different-signing-secret-not-shared-1234567890");
        ReflectionTestUtils.setField(otherIssuer, "expirationMs", 3600000L);
        String tokenFromSomewhereElse = otherIssuer.generateToken("demo");

        assertThatThrownBy(() -> jwtUtil.validateAndGetSubject(tokenFromSomewhereElse))
                .isInstanceOf(JwtException.class);
    }

    @Test
    void validateAndGetSubject_rejectsAnExpiredToken() {
        ReflectionTestUtils.setField(jwtUtil, "expirationMs", -1000L); // already expired the instant it's issued
        String expiredToken = jwtUtil.generateToken("demo");

        assertThatThrownBy(() -> jwtUtil.validateAndGetSubject(expiredToken))
                .isInstanceOf(JwtException.class);
    }

    @Test
    void validateAndGetSubject_rejectsGarbage() {
        assertThatThrownBy(() -> jwtUtil.validateAndGetSubject("not-a-jwt-at-all"))
                .isInstanceOf(JwtException.class);
    }
}
