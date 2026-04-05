package org.openfamilycompass.config;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.openfamilycompass.security.RateLimitingFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import lombok.RequiredArgsConstructor;

@Configuration
@EnableWebSecurity
@Profile("!test")
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

        private final RateLimitingFilter rateLimitingFilter;

        @Bean
        public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
                return config.getAuthenticationManager();
        }

        @Bean
        public JwtAuthenticationConverter jwtAuthenticationConverter() {
                JwtAuthenticationConverter converter = new JwtAuthenticationConverter();

                // Custom converter for "roles" claim in JWT
                converter.setJwtGrantedAuthoritiesConverter(jwt -> {
                        // Standard scopes
                        JwtGrantedAuthoritiesConverter scopeConverter = new JwtGrantedAuthoritiesConverter();
                        Collection<? extends GrantedAuthority> scopeAuthorities = scopeConverter.convert(jwt);

                        // Custom roles aus "roles" Claim
                        List<String> roles = jwt.getClaimAsStringList("roles");

                        // Handle roles list being null
                        if (roles == null) {
                                roles = new java.util.ArrayList<>();
                        } else {
                                // Ensure roles is a mutable list
                                roles = new java.util.ArrayList<>(roles);
                        }

                        // If ADMIN role is present, add PARENT role (hierarchy)
                        if (roles.contains("ADMIN") && !roles.contains("PARENT")) {
                                roles.add("PARENT");
                        }

                        Collection<GrantedAuthority> roleAuthorities = roles.stream()
                                        .map(role -> (GrantedAuthority) new SimpleGrantedAuthority("ROLE_" + role))
                                        .collect(Collectors.toList());

                        // Combine scope and role authorities
                        return Stream.concat(
                                        scopeAuthorities != null ? scopeAuthorities.stream() : Stream.empty(),
                                        roleAuthorities.stream()).collect(Collectors.toSet());
                });

                return converter;
        }

        @Bean
        public CorsConfigurationSource corsConfigurationSource() {
                CorsConfiguration configuration = new CorsConfiguration();
                configuration.setAllowedOriginPatterns(java.util.List.of("*")); // Allow all origins for WebView
                configuration.setAllowedMethods(
                                java.util.List.of("GET", "POST", "PUT", "DELETE", "OPTIONS", "HEAD", "PATCH"));
                configuration.setAllowedHeaders(java.util.List.of("*"));
                configuration.setExposedHeaders(java.util.List.of("Authorization"));
                configuration.setAllowCredentials(true);
                UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
                source.registerCorsConfiguration("/**", configuration); // Apply to all endpoints
                return source;
        }

        @Bean
        public SecurityFilterChain apiFilterChain(HttpSecurity http,
                        JwtAuthenticationConverter jwtAuthenticationConverter) throws Exception {
                http
                                .cors(Customizer.withDefaults()) // CORS aktivieren
                                .addFilterBefore(rateLimitingFilter, UsernamePasswordAuthenticationFilter.class)
                                .authorizeHttpRequests(auth -> auth
                                                // Public static resources
                                                .requestMatchers("/css/**", "/js/**", "/images/**", "/avatar/**",
                                                                "/favicon.png", "/favicon.ico")
                                                .permitAll()
                                                // Swagger UI
                                                .requestMatchers("/swagger-ui/**", "/swagger-ui.html",
                                                                "/v3/api-docs/**")
                                                .permitAll()
                                                // Actuator
                                                .requestMatchers("/actuator/**").permitAll()
                                                // Auth Endpoints (LOGIN)
                                                .requestMatchers("/api/auth/**").permitAll()
                                                .requestMatchers("/api/v1/auth/**").permitAll()
                                                .requestMatchers("/api/avatar/icons").permitAll()
                                                // Reward images are public (not sensitive)
                                                .requestMatchers(org.springframework.http.HttpMethod.GET,
                                                                "/api/v1/rewards/*/image")
                                                .permitAll()
                                                .requestMatchers("/error").permitAll()
                                                // All other requests need authentication
                                                .anyRequest().authenticated())
                                .oauth2ResourceServer(oauth2 -> oauth2
                                                .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter)))
                                .sessionManagement(session -> session
                                                .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                                .csrf(csrf -> csrf.disable())
                                .exceptionHandling(handling -> handling
                                                .authenticationEntryPoint(
                                                                new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)));

                return http.build();
        }

}
