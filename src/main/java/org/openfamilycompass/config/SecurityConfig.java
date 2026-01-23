package org.openfamilycompass.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.security.web.util.matcher.NegatedRequestMatcher;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import lombok.RequiredArgsConstructor;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

        @Bean
        public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
                return config.getAuthenticationManager();
        }

        @Bean
        public CorsConfigurationSource corsConfigurationSource() {
                CorsConfiguration configuration = new CorsConfiguration();
                configuration.setAllowedOriginPatterns(java.util.List.of("*")); // Erlaube alle Origins für WebView
                configuration.setAllowedMethods(java.util.List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
                configuration.setAllowedHeaders(java.util.List.of("*"));
                configuration.setAllowCredentials(true);
                UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
                source.registerCorsConfiguration("/api/**", configuration);
                source.registerCorsConfiguration("/notifications/**", configuration); // Für WebView
                return source;
        }

        // API Security: Stateless with OIDC JWT
        @Bean
        @Order(3)
        public SecurityFilterChain apiFilterChain(HttpSecurity http) throws Exception {
                http
                                .securityMatcher("/api/**")
                                .cors(Customizer.withDefaults()) // CORS aktivieren
                                .authorizeHttpRequests(auth -> auth
                                                .requestMatchers("/api/auth/**").permitAll()
                                                .requestMatchers("/api/avatar/icons").permitAll()
                                                .anyRequest().authenticated())
                                .oauth2ResourceServer(oauth2 -> oauth2.jwt(Customizer.withDefaults()))
                                .sessionManagement(session -> session
                                                .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                                .csrf(csrf -> csrf.disable())
                                .exceptionHandling(handling -> handling.authenticationEntryPoint((req, res, ex) -> {
                                        res.sendError(401, "Unauthorized");
                                }));

                return http.build();
        }

        // Web UI Security: Stateful with form login - ONLY for non-API requests
        @Bean
        @Order(2)
        public SecurityFilterChain webFilterChain(HttpSecurity http)
                        throws Exception {

                http
                                .securityMatcher(new NegatedRequestMatcher(
                                                PathPatternRequestMatcher.withDefaults().matcher("/api/**")))
                                .cors(Customizer.withDefaults()) // CORS für WebView
                                .authorizeHttpRequests(auth -> auth
                                                .requestMatchers("/css/**", "/js/**", "/images/**", "/avatar/**",
                                                                "/favicon.png", "/favicon.ico",
                                                                "/swagger-ui/**", "/swagger-ui.html", "/v3/api-docs/**")
                                                .permitAll()
                                                .requestMatchers("/login", "/error").permitAll()
                                                .requestMatchers("/profile/**").authenticated()
                                                .requestMatchers("/admin/**").hasRole("ADMIN")
                                                .requestMatchers("/parent/**").hasAnyRole("ADMIN", "PARENT")
                                                .requestMatchers("/child/**").hasRole("CHILD")
                                                .anyRequest().authenticated())
                                .sessionManagement(session -> session
                                                .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))
                                .formLogin(form -> form
                                                .loginPage("/login")
                                                .loginProcessingUrl("/perform_login")
                                                .defaultSuccessUrl("/dashboard", true)
                                                .failureUrl("/login?error=true")
                                                .permitAll())
                                .rememberMe(remember -> remember
                                                .key("openFamilyCompassKey")
                                                .tokenValiditySeconds(86400 * 30) // 30 Tage
                                                .rememberMeParameter("remember-me"))
                                .logout(logout -> logout
                                                .logoutUrl("/logout")
                                                .logoutSuccessUrl("/")
                                                .invalidateHttpSession(true)
                                                .deleteCookies("JSESSIONID")
                                                .permitAll())
                                .csrf(csrf -> csrf.ignoringRequestMatchers("/api/**", "/notifications/**"));

                return http.build();
        }
}
