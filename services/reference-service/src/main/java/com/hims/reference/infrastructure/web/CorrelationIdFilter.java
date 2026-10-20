package com.hims.reference.infrastructure.web;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * Correlation-id filter: accepts the legacy {@code X-Request-ID} header
 * (w-api-call.js convention, STRANGLER §2.1) or generates one; propagates in MDC
 * ({@code corr}) and echoes it back on the response. Log pattern includes %X{corr}.
 */
@Component
public class CorrelationIdFilter extends OncePerRequestFilter {

    public static final String CORRELATION_ID_HEADER = "X-Request-ID";

    /** F-17 (AGT-10): cap echoed correlation ids (header echo hygiene). */
    public static final int MAX_CORRELATION_ID_LENGTH = 128;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String correlationId = request.getHeader(CORRELATION_ID_HEADER);
        if (correlationId == null || correlationId.isBlank()) {
            correlationId = UUID.randomUUID().toString();
        } else if (correlationId.length() > MAX_CORRELATION_ID_LENGTH) {
            correlationId = correlationId.substring(0, MAX_CORRELATION_ID_LENGTH);
        }
        try (MDC.MDCCloseable ignored = MDC.putCloseable("corr", correlationId)) {
            response.setHeader(CORRELATION_ID_HEADER, correlationId);
            chain.doFilter(request, response);
        }
    }
}
