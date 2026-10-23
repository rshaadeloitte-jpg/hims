package com.hims.reference.configuration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

import jakarta.servlet.http.HttpServletResponse;

/**
 * DEV-ONLY authentication chain (IMP-DEC-019): enables HTTP Basic so the Wave 1
 * React workspace can exercise the API locally while MIG-004 IAM is pending.
 * <p>
 * Guards: active ONLY with Spring profile {@code dev}; matches the exact same
 * permitAll set as the default chain (actuator health + OpenAPI docs); every
 * business endpoint still requires authentication — no anonymous access added.
 * Credentials from {@code spring.security.user.*} (env-overridable, dev-only
 * defaults). This class is REMOVED at MIG-004 when OIDC resource-server config lands.
 */
@Profile("dev")
@Configuration
public class DevSecurityConfig {

    @Bean
    @Order(1)
    public SecurityFilterChain devSecurityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(eh -> eh.authenticationEntryPoint(
                        (request, response, ex) -> response.sendError(HttpServletResponse.SC_UNAUTHORIZED)))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/actuator/health/**",
                                "/v3/api-docs/**",
                                "/swagger-ui/**",
                                "/swagger-ui.html",
                                "/error").permitAll()
                        .requestMatchers("/admin/**").hasRole("ADMIN")
                        .anyRequest().authenticated())
                .httpBasic(c -> c.realmName("ihms-dev"))
                .formLogin(AbstractHttpConfigurer::disable);
        return http.build();
    }
}
