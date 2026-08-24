package com.payment.transaction.config;

import feign.RequestInterceptor;
import io.micrometer.tracing.Span;
import io.micrometer.tracing.Tracer;
import io.micrometer.tracing.propagation.Propagator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Spring Cloud OpenFeign's automatic Micrometer Observation instrumentation
 * (which is supposed to propagate trace context onto outgoing Feign calls
 * for free once micrometer-tracing is on the classpath) didn't fire in
 * this Spring Boot 3.2.0 / Spring Cloud 2023.0.0 combination when this was
 * checked against live logs - each service kept generating its own,
 * disconnected trace ID instead of continuing the caller's. This interceptor
 * does the propagation explicitly: it writes the current span's context
 * onto every outgoing Feign request's headers, the same way a real gateway
 * or service mesh does it, so one transaction's trace ID is visible in
 * every service's logs it passes through, not just its own.
 */
@Configuration
public class FeignTracingConfig {

    @Bean
    public RequestInterceptor tracingPropagationInterceptor(Tracer tracer, Propagator propagator) {
        return requestTemplate -> {
            Span currentSpan = tracer.currentSpan();
            if (currentSpan != null) {
                propagator.inject(currentSpan.context(), requestTemplate,
                        (carrier, key, value) -> carrier.header(key, value));
            }
        };
    }
}
