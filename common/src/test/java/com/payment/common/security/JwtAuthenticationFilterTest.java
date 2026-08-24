package com.payment.common.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.PrintWriter;
import java.io.StringWriter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * This filter is component-scanned into every service in the flow and
 * gates every /api/** endpoint - a mistake here breaks the whole system at
 * once (which is exactly what happened once already: shouldNotFilter's
 * first version had no static-resource exemption and blocked the
 * dashboard's own index.html, discovered only by loading it in a browser).
 */
class JwtAuthenticationFilterTest {

    private JwtUtil jwtUtil;
    private JwtAuthenticationFilter filter;

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil();
        ReflectionTestUtils.setField(jwtUtil, "secret", "test-only-hmac-signing-secret-not-a-real-secret-1234567890");
        ReflectionTestUtils.setField(jwtUtil, "expirationMs", 3600000L);
        filter = new JwtAuthenticationFilter(jwtUtil);
    }

    @Test
    void shouldNotFilter_letsStaticDashboardResourcesThrough() {
        assertThat(filter.shouldNotFilter(requestFor("/dashboard/index.html"))).isTrue();
    }

    @Test
    void shouldNotFilter_letsHealthChecksThrough() {
        assertThat(filter.shouldNotFilter(requestFor("/api/pos/health"))).isTrue();
    }

    @Test
    void shouldNotFilter_letsTokenIssuanceThrough() {
        assertThat(filter.shouldNotFilter(requestFor("/api/auth/token"))).isTrue();
    }

    @Test
    void shouldNotFilter_gatesEveryOtherApiPath() {
        assertThat(filter.shouldNotFilter(requestFor("/api/transactions/authorize"))).isFalse();
    }

    @Test
    void shouldNotFilter_letsCorsPreflightRequestsThrough() {
        // Browsers strip the Authorization header from an OPTIONS preflight,
        // so gating it here would 401 before Spring's CORS handling ever
        // runs - the browser reports that as a missing CORS header, not a
        // 401, which is exactly the bug this test pins down.
        HttpServletRequest request = requestFor("/api/transactions");
        when(request.getMethod()).thenReturn("OPTIONS");

        assertThat(filter.shouldNotFilter(request)).isTrue();
    }

    @Test
    void doFilterInternal_rejectsARequestWithNoToken() throws Exception {
        HttpServletRequest request = requestFor("/api/transactions");
        when(request.getHeader("Authorization")).thenReturn(null);
        HttpServletResponse response = mockResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilterInternal(request, response, chain);

        verify(response).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        verifyNoInteractions(chain);
    }

    @Test
    void doFilterInternal_rejectsAnInvalidToken() throws Exception {
        HttpServletRequest request = requestFor("/api/transactions");
        when(request.getHeader("Authorization")).thenReturn("Bearer not-a-real-token");
        HttpServletResponse response = mockResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilterInternal(request, response, chain);

        verify(response).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        verifyNoInteractions(chain);
    }

    @Test
    void doFilterInternal_lestARequestWithAValidTokenThrough() throws Exception {
        String token = jwtUtil.generateToken("demo");
        HttpServletRequest request = requestFor("/api/transactions");
        when(request.getHeader("Authorization")).thenReturn("Bearer " + token);
        HttpServletResponse response = mockResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilterInternal(request, response, chain);

        verify(chain).doFilter(request, response);
        verify(response, never()).setStatus(anyInt());
    }

    private HttpServletRequest requestFor(String uri) {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getRequestURI()).thenReturn(uri);
        return request;
    }

    private HttpServletResponse mockResponse() throws Exception {
        HttpServletResponse response = mock(HttpServletResponse.class);
        when(response.getWriter()).thenReturn(new PrintWriter(new StringWriter()));
        return response;
    }
}
