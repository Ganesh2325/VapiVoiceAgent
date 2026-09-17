package com.voiceos.config;

import com.voiceos.security.JsonAccessDeniedHandler;
import com.voiceos.security.JsonAuthEntryPoint;
import com.voiceos.security.JwtAuthenticationFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.ArrayList;
import java.util.List;

/**
 * Spring Security configuration.
 *
 * <p>Endpoint classification for {@code /api/v1/**}:
 * <ul>
 *   <li>PUBLIC — login, register, token refresh, actuator health/info, OpenAPI</li>
 *   <li>AUTHENTICATED — conversations, tasks, memory, tools, approvals, agents, metrics, system, executions</li>
 *   <li>VAPI / INTERNAL — webhook paths; JWT is not used. Authenticity is the Vapi shared secret.</li>
 * </ul>
 *
 * <p>CSRF decision: VoiceOS uses stateless JWT bearer tokens in the
 * {@code Authorization} header. The API does not issue authentication cookies,
 * so browser CSRF against cookie-authenticated sessions does not apply.
 * CSRF protection is therefore disabled. If cookie-based session auth is
 * introduced later, CSRF must be re-enabled for those cookie flows.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthFilter;
    private final UserDetailsService userDetailsService;
    private final VoiceOsProperties voiceOsProperties;
    private final JsonAuthEntryPoint jsonAuthEntryPoint;
    private final JsonAccessDeniedHandler jsonAccessDeniedHandler;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthFilter,
                          UserDetailsService userDetailsService,
                          VoiceOsProperties voiceOsProperties,
                          JsonAuthEntryPoint jsonAuthEntryPoint,
                          JsonAccessDeniedHandler jsonAccessDeniedHandler) {
        this.jwtAuthFilter = jwtAuthFilter;
        this.userDetailsService = userDetailsService;
        this.voiceOsProperties = voiceOsProperties;
        this.jsonAuthEntryPoint = jsonAuthEntryPoint;
        this.jsonAccessDeniedHandler = jsonAccessDeniedHandler;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .exceptionHandling(ex -> ex
                .authenticationEntryPoint(jsonAuthEntryPoint)
                .accessDeniedHandler(jsonAccessDeniedHandler))
            .authorizeHttpRequests(auth -> auth
                // PUBLIC — authentication
                .requestMatchers(HttpMethod.POST, "/api/v1/auth/register", "/api/v1/auth/login").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/v1/auth/refresh").permitAll()
                // PUBLIC — health and docs
                .requestMatchers("/actuator/health", "/actuator/info").permitAll()
                .requestMatchers("/swagger-ui/**", "/api-docs/**", "/v3/api-docs/**").permitAll()
                // PUBLIC handshake — origin is still restricted by WebSocketConfig
                .requestMatchers("/ws/**").permitAll()
                // VAPI — authenticated via x-vapi-secret, not JWT
                .requestMatchers("/api/webhooks/**", "/api/v1/webhooks/**").permitAll()
                // AUTHENTICATED — all remaining /api/v1/** (tools, approvals, tasks, memory, conversations, agents, metrics, system)
                .anyRequest().authenticated()
            )
            .authenticationProvider(authenticationProvider())
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config)
            throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(resolveAllowedOrigins());
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
        config.setAllowedHeaders(List.of("Authorization", "Content-Type", "X-Request-Id", "x-vapi-secret"));
        config.setExposedHeaders(List.of("X-Request-Id"));
        config.setAllowCredentials(true);
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    /**
     * Explicit origin list. Wildcard {@code *} is rejected because authenticated
     * traffic must not be exposed to every origin.
     */
    public List<String> resolveAllowedOrigins() {
        List<String> configured = new ArrayList<>();
        if (voiceOsProperties.security() != null
                && voiceOsProperties.security().cors() != null
                && voiceOsProperties.security().cors().allowedOrigins() != null) {
            configured.addAll(voiceOsProperties.security().cors().allowedOrigins());
        }
        List<String> allowed = configured.stream()
                .filter(origin -> origin != null && !origin.isBlank())
                .map(String::trim)
                .filter(origin -> !"*".equals(origin))
                .distinct()
                .toList();
        if (allowed.isEmpty()) {
            return List.of("http://localhost:3000", "http://localhost:5173");
        }
        return allowed;
    }
}
