package com.hims.bloodbank.infrastructure.web;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/** Propagates X-Correlation-Id into the MDC for log correlation (sibling convention). */
@Component
public class CorrelationIdFilter extends OncePerRequestFilter {

    public static final String HEADER = "X-Correlation-Id";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        String corr = request.getHeader(HEADER);
        if (corr == null || corr.isBlank()) {
            corr = UUID.randomUUID().toString();
        }
        MDC.put("corr", corr);
        response.setHeader(HEADER, corr);
        try {
            chain.doFilter(request, response);
        } finally {
            MDC.remove("corr");
        }
    }
}
