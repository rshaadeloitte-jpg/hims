package com.hims.admin.configuration;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

/** DEV-ONLY HTTP Basic chain: rmrs/reporting prefixes permitAll, forms stays authenticated. */
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
                        .requestMatchers("/api/v1/forms/**").authenticated()
                        .requestMatchers(
                                "/api/v1/collections/**",
                                "/api/v1/masters/**",
                                "/api/v1/rmrs/**",
                                "/api/v1/bank/**",
                                "/api/v1/po/**",
                                "/api/v1/journals/**",
                                "/api/v1/reports/**",
                                "/api/v1/dashboard/**",
                                "/api/v1/internal/**",
                                "/api/reports/janta/**",
                                "/api/reports/operational/**",
                                "/api/reports/statistical/**").permitAll()
                        .requestMatchers("/admin/**").hasRole("ADMIN")
                        .anyRequest().authenticated())
                .httpBasic(c -> c.realmName("ihms-dev"))
                .formLogin(AbstractHttpConfigurer::disable);
        return http.build();
    }
}
