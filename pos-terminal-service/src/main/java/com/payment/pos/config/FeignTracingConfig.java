package com.payment.pos.config;

import feign.RequestInterceptor;
import io.micrometer.tracing.Span;
import io.micrometer.tracing.Tracer;
import io.micrometer.tracing.propagation.Propagator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Spring Cloud OpenFeign's automatic Micrometer Observation instrumentation
 * didn't propagate trace context onto outgoing Feign calls in this Spring
 * Boot 3.2.0 / Spring Cloud 2023.0.0 combination when checked against live
 * logs - see transaction-service's identical class for the full story.
 * This does the propagation explicitly, so a POS transaction's trace ID
 * carries all the way into transaction-service's logs (and from there,
 * merchant/acquirer/network/issuer-service's too).
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
