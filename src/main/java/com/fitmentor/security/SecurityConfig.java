package com.fitmentor.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fitmentor.api.error.ApiError;
import com.fitmentor.config.FitmentorProperties;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.io.IOException;
import java.util.List;

/**
 * Stateless API security. Public: health, auth and the WebSocket handshake. Everything else
 * requires a bearer token and answers 401 with the {@link ApiError} body when it is missing.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    public static final String[] PUBLIC_PATHS = {
        "/api/health", "/actuator/health", "/actuator/health/**", "/api/auth/**", "/ws", "/ws/**", "/error"
    };

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, JwtService jwtService, ObjectMapper objectMapper,
                                                   FitmentorProperties properties) throws Exception {
        return http
            .csrf(csrf -> csrf.disable())
            .cors(cors -> cors.configurationSource(corsConfigurationSource(properties)))
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                .requestMatchers(PUBLIC_PATHS).permitAll()
                .anyRequest().authenticated())
            .exceptionHandling(handling -> handling.authenticationEntryPoint(unauthorized(objectMapper)))
            .addFilterBefore(new JwtAuthenticationFilter(jwtService), UsernamePasswordAuthenticationFilter.class)
            .build();
    }

    /** CORS from {@code FRONTEND_ORIGIN}; a single "*" allows any origin. */
    static CorsConfigurationSource corsConfigurationSource(FitmentorProperties properties) {
        List<String> origins = properties.cors().allowedOrigins();
        CorsConfiguration configuration = new CorsConfiguration();
        if (origins.contains("*")) {
            configuration.addAllowedOriginPattern("*");
        } else {
            origins.forEach(configuration::addAllowedOrigin);
        }
        configuration.setAllowedMethods(List.of("GET", "POST", "PATCH", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("Authorization", "Content-Type"));
        configuration.setAllowCredentials(false);
        configuration.setMaxAge(3600L);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    private static AuthenticationEntryPoint unauthorized(ObjectMapper objectMapper) {
        return (HttpServletRequest request, HttpServletResponse response, org.springframework.security.core.AuthenticationException ex) -> {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            try {
                objectMapper.writeValue(response.getOutputStream(),
                    new ApiError("unauthorized", "Authentication is required."));
            } catch (IOException ioException) {
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
            }
        };
    }
}
