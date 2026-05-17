package com.team35.freelance.contract.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.util.matcher.RegexRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;

// el correlation filter mokamel fel servlet chain (FilterRegistrationBean) abl el security — hina el JWT bas
@Configuration
public class SecurityConfig {

    private static final Logger log = LoggerFactory.getLogger(SecurityConfig.class);

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        log.info("CUSTOM CONTRACT SECURITY CONFIG LOADED");

        http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.GET, "/actuator/health", "/api/health").permitAll()
                        .requestMatchers(publicInternalGetEndpoints()).permitAll()
                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    private RequestMatcher[] publicInternalGetEndpoints() {
        return new RequestMatcher[] {
                get("^/api/contracts/[0-9]+$"),
                get("^/api/contracts/user/[0-9]+/summary$"),
                get("^/api/contracts/user/[0-9]+/active-count$"),
                get("^/api/contracts/user/[0-9]+/completed-count$"),
                get("^/api/contracts/job/[0-9]+/active-count$"),
                get("^/api/contracts/proposal/[0-9]+/active$")
        };
    }

    private RequestMatcher get(String pattern) {
        return RegexRequestMatcher.regexMatcher(HttpMethod.GET, pattern);
    }
}
