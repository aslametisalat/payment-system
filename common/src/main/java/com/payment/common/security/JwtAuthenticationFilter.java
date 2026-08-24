package com.payment.common.security;

import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Rejects any request without a valid bearer token, on every service that
 * component-scans com.payment.common.security into its context. Requests
 * for health checks, actuator, API docs, and (obviously) the token-
 * issuance endpoint itself are let through unauthenticated - everything
 * else under /api/** needs "Authorization: Bearer <token>" from a token
 * security-service issued (see AuthController there).
 *
 * This intentionally stops at "is the token validly signed and
 * unexpired" - there's no role/permission model here (every valid token
 * can call every endpoint), matching the demo/single-tenant scope of this
 * project. A real system would check the token's claims against what the
 * endpoint requires, not just that it has one.
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationFilter.class);

    private final JwtUtil jwtUtil;

    public JwtAuthenticationFilter(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        // CORS preflight requests never carry the app's Authorization header
        // (browsers strip it) - rejecting them here would return 401 before
        // Spring's CORS handling ever runs, which the browser reports as a
        // missing Access-Control-Allow-Origin header, not as a 401. Real
        // authorization for the actual request still happens below;
        // OPTIONS never reaches a controller either way.
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }
        String path = request.getRequestURI();
        // Only /api/** is gated at all - static resources (the dashboard's
        // own HTML/CSS/JS, served by pos-terminal-service) and everything
        // else pass through untouched.
        if (!path.contains("/api/")) {
            return true;
        }
        return path.contains("/health")
                || path.contains("/actuator")
                || path.contains("/swagger")
                || path.contains("/v3/api-docs")
                || path.contains("/h2-console")
                || path.contains("/api/auth/token");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        if (header == null || !header.startsWith("Bearer ")) {
            reject(response, "Missing bearer token - call POST /api/auth/token on security-service first");
            return;
        }
        try {
            jwtUtil.validateAndGetSubject(header.substring("Bearer ".length()));
        } catch (JwtException | IllegalArgumentException e) {
            log.debug("Rejected request to {}: {}", request.getRequestURI(), e.getMessage());
            reject(response, "Invalid or expired token");
            return;
        }
        chain.doFilter(request, response);
    }

    private void reject(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json");
        response.getWriter().write("{\"error\":\"" + message + "\"}");
    }
}
