package com.cooperativa.met.infrastructure.security;

import java.util.List;

import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import com.cooperativa.met.infrastructure.config.MetCorsProperties;
import com.cooperativa.met.infrastructure.config.RateLimitProperties;
import com.fasterxml.jackson.databind.ObjectMapper;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final MetCorsProperties corsProperties;
    private final RateLimitProperties rateLimitProperties;
    private final ObjectMapper objectMapper;
    private final RedisRateLimiter redisRateLimiter;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter,
                          MetCorsProperties corsProperties,
                          RateLimitProperties rateLimitProperties,
                          ObjectMapper objectMapper,
                          RedisRateLimiter redisRateLimiter) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.corsProperties = corsProperties;
        this.rateLimitProperties = rateLimitProperties;
        this.objectMapper = objectMapper;
        this.redisRateLimiter = redisRateLimiter;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/actuator/health", "/actuator/info").permitAll()
                        .requestMatchers(HttpMethod.POST, "/v1/auth/register", "/v1/auth/login").permitAll()
                        .requestMatchers(HttpMethod.POST, "/v1/auth/biometric").permitAll()
                        .requestMatchers(HttpMethod.POST, "/v1/auth/refresh").permitAll()
                        .requestMatchers(HttpMethod.POST, "/v1/loans/simulate").permitAll()
                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /**
     * Registra el filtro de rate limiting ANTES del filtro de Spring Security,
     * con orden 1 para que sea el primero en ejecutarse.
     */
    @Bean
    public FilterRegistrationBean<AuthRateLimitFilter> rateLimitFilterRegistration() {
        FilterRegistrationBean<AuthRateLimitFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(new AuthRateLimitFilter(rateLimitProperties, objectMapper, redisRateLimiter));
        registration.addUrlPatterns("/api/v1/auth/*");
        registration.setOrder(1);
        registration.setName("authRateLimitFilter");
        return registration;
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(corsProperties.getAllowedOrigins());
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
