package com.enterprise.user_management.security;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
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

import java.util.Arrays;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig {

    @Autowired
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(Arrays.asList("http://localhost:4200"));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
        configuration.setAllowedHeaders(Arrays.asList("*"));
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", configuration);
        return source;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource())) // Add CORS support
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // Public endpoints - Authentication not required
                        .requestMatchers("/api/auth/**").permitAll()
                        .requestMatchers("/api/users/health").permitAll()

                        // User registration
                        .requestMatchers(HttpMethod.POST, "/api/users").permitAll()

                        // ==== TEMPORARY FOR DEVELOPMENT (Day 10-12 Frontend Integration) ====
                        // TODO: Remove these permits after authentication is implemented in frontend
                        .requestMatchers(HttpMethod.GET, "/api/users").permitAll()        // Get paginated users
                        .requestMatchers(HttpMethod.GET, "/api/users/all").permitAll()   // Get all users
                        .requestMatchers(HttpMethod.GET, "/api/users/stats").permitAll() // Get user stats
                        .requestMatchers(HttpMethod.GET, "/api/users/search").permitAll() // Search users
                        .requestMatchers(HttpMethod.GET, "/api/users/*").permitAll()     // Get user by ID (FIXED: removed trailing slash)
                        .requestMatchers(HttpMethod.GET, "/api/users/username/*").permitAll() // Get by username
                        .requestMatchers(HttpMethod.GET, "/api/users/role/*").permitAll() // Get by role
                        .requestMatchers(HttpMethod.PUT, "/api/users/*").permitAll()     // Update user (Day 12)
                        .requestMatchers(HttpMethod.PATCH, "/api/users/*/activate").permitAll() // Activate user
                        .requestMatchers(HttpMethod.PATCH, "/api/users/*/deactivate").permitAll() // Deactivate user
                        .requestMatchers(HttpMethod.DELETE, "/api/users/*").permitAll()  // Delete user (Day 12)
                        // ===================================================================

                        // Swagger/OpenAPI endpoints
                        .requestMatchers("/swagger-ui/**").permitAll()
                        .requestMatchers("/swagger-ui.html").permitAll()
                        .requestMatchers("/v3/api-docs/**").permitAll()
                        .requestMatchers("/v3/api-docs").permitAll()
                        .requestMatchers("/api-docs/**").permitAll()
                        .requestMatchers("/swagger-resources/**").permitAll()
                        .requestMatchers("/webjars/**").permitAll()
                        .requestMatchers("/swagger-ui-custom.html").permitAll()
                        .requestMatchers("/swagger-initializer.js").permitAll()

                        // Actuator endpoints
                        .requestMatchers("/actuator/**").permitAll()

                        // Static resources
                        .requestMatchers("/css/**", "/js/**", "/images/**", "/favicon.ico").permitAll()

                        // Admin only endpoints (keep these protected)
                        // Note: Activate/deactivate are now temporarily allowed above for development

                        // All other requests require authentication
                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}