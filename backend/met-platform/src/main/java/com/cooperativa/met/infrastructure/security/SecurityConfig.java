package com.cooperativa.met.infrastructure.security;

import java.util.List;

import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
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
@EnableMethodSecurity
@org.springframework.boot.context.properties.EnableConfigurationProperties({MetCorsProperties.class, RateLimitProperties.class, com.cooperativa.met.infrastructure.config.MetSecurityProperties.class})
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final MetCorsProperties corsProperties;
    private final RateLimitProperties rateLimitProperties;
    private final ObjectMapper objectMapper;
    private final RedisRateLimiter redisRateLimiter;
    private final HmacSignatureFilter hmacSignatureFilter;
    private final GeoBlockingFilter geoBlockingFilter;
    private final TurnstileValidationService turnstileValidationService;
    private final DeviceAttestationService attestationService;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter,
                          MetCorsProperties corsProperties,
                          RateLimitProperties rateLimitProperties,
                          ObjectMapper objectMapper,
                          RedisRateLimiter redisRateLimiter,
                          HmacSignatureFilter hmacSignatureFilter,
                          GeoBlockingFilter geoBlockingFilter,
                          TurnstileValidationService turnstileValidationService,
                          DeviceAttestationService attestationService) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.corsProperties = corsProperties;
        this.rateLimitProperties = rateLimitProperties;
        this.objectMapper = objectMapper;
        this.redisRateLimiter = redisRateLimiter;
        this.hmacSignatureFilter = hmacSignatureFilter;
        this.geoBlockingFilter = geoBlockingFilter;
        this.turnstileValidationService = turnstileValidationService;
        this.attestationService = attestationService;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                // Headers de seguridad HTTP estándar para aplicaciones financieras
                .headers(headers -> headers
                        // Evita que el navegador adivine el tipo MIME (protege contra content-sniffing)
                        .contentTypeOptions(contentTypeOptions -> {})
                        // Prohíbe que la app sea embebida en iframes externos (anti-clickjacking)
                        .frameOptions(frameOptions -> frameOptions.deny())
                        // Fuerza HTTPS por 1 año e incluye subdominios
                        .httpStrictTransportSecurity(hsts -> hsts
                                .includeSubDomains(true)
                                .maxAgeInSeconds(31536000)
                        )
                        // Política de contenido: solo permite recursos del mismo origen
                        .contentSecurityPolicy(csp -> csp
                                .policyDirectives("default-src 'self'; frame-ancestors 'none'; object-src 'none'")
                        )
                )
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/actuator/health", "/actuator/info").permitAll()
                        // Permitir preflight CORS
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        // Rutas públicas de usuarios
                        .requestMatchers(HttpMethod.GET, "/v1/auth/public-key").permitAll()
                        .requestMatchers(HttpMethod.POST, "/v1/auth/register", "/v1/auth/login").permitAll()
                        .requestMatchers(HttpMethod.POST, "/v1/auth/pin-recovery/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/v1/auth/verify-email").permitAll()
                        .requestMatchers(HttpMethod.POST, "/v1/auth/resend-email-otp").permitAll()
                        .requestMatchers(HttpMethod.POST, "/v1/auth/refresh").permitAll()
                        .requestMatchers(HttpMethod.POST, "/v1/loans/simulate").permitAll()
                        .requestMatchers(HttpMethod.POST, "/support/chat").permitAll()
                        .requestMatchers(HttpMethod.POST, "/support/public/contact").permitAll()
                        // Simulador de pasarela de pagos (se abre en navegador sin token)
                        .requestMatchers("/v1/mock-payment-gateway").permitAll()
                        // Webhooks de Wompi — no llevan JWT, la seguridad es por firma HMAC
                        .requestMatchers(HttpMethod.POST, "/v1/webhooks/payment").permitAll()
                        // Webhook simulado para desarrollo local (MockPaymentGateway)
                        .requestMatchers(HttpMethod.POST, "/v1/webhooks/mock-payment").permitAll()
                        // Login público del admin
                        .requestMatchers(HttpMethod.POST, "/v1/admin/auth/login").permitAll()
                        // Swagger UI — solo en perfil dev (controlado por @Profile en OpenApiConfig)
                        .requestMatchers("/swagger-ui/**", "/swagger-ui.html", "/v3/api-docs/**").permitAll()
                        // Rutas admin — requieren rol ADMIN o SUPER_ADMIN
                        .requestMatchers("/v1/admin/**").hasAnyRole("ADMIN", "SUPER_ADMIN")
                        .anyRequest().authenticated()
                )
                .exceptionHandling(ex -> ex.authenticationEntryPoint(
                        new org.springframework.security.web.authentication.HttpStatusEntryPoint(org.springframework.http.HttpStatus.UNAUTHORIZED)
                ))
                .addFilterBefore(geoBlockingFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterAfter(jwtAuthenticationFilter, GeoBlockingFilter.class)
                .addFilterAfter(hmacSignatureFilter, JwtAuthenticationFilter.class);

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
        registration.addUrlPatterns("/v1/*");
        registration.setOrder(2); // Despues de Turnstile
        registration.setName("authRateLimitFilter");
        return registration;
    }

    @Bean
    public FilterRegistrationBean<TurnstileFilter> turnstileFilterRegistration() {
        FilterRegistrationBean<TurnstileFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(new TurnstileFilter(turnstileValidationService, objectMapper));
        registration.addUrlPatterns("/v1/*");
        registration.setOrder(1); // Antes del Rate Limit
        registration.setName("turnstileFilter");
        return registration;
    }

    @Bean
    public FilterRegistrationBean<DeviceAttestationFilter> deviceAttestationFilterRegistration() {
        FilterRegistrationBean<DeviceAttestationFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(new DeviceAttestationFilter(attestationService, objectMapper));
        registration.addUrlPatterns("/v1/*");
        registration.setOrder(3); // Despues de Autenticacion/RateLimit
        registration.setName("deviceAttestationFilter");
        return registration;
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOriginPatterns(corsProperties.getAllowedOrigins());
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
