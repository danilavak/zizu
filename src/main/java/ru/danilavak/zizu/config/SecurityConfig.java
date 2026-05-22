package ru.danilavak.zizu.config;

import org.springframework.http.HttpMethod;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;

@Configuration
@EnableWebSecurity
public class SecurityConfig {
    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    }

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .formLogin(form -> form.disable())
                .addFilterBefore(jwtAuthenticationFilter, BasicAuthenticationFilter.class)
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/error").permitAll()
                        .requestMatchers("/auth/register", "/auth/login", "/auth/refresh").permitAll()
                        .requestMatchers("/actuator/health", "/actuator/info").permitAll()
                        .requestMatchers("/signature/certificate").permitAll()
                        .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/binary/signatures/full", "/api/binary/signatures/increment").hasAnyRole("ADMIN", "USER")
                        .requestMatchers(HttpMethod.POST, "/api/binary/signatures/by-ids").hasAnyRole("ADMIN", "USER")
                        .requestMatchers(HttpMethod.POST, "/malware-signature-files/upload", "/malware-signature-files/presigned-urls").hasRole("ADMIN")
                        .requestMatchers("/malware-signatures/*/history", "/malware-signatures/*/audit").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.POST, "/malware-signatures").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/malware-signatures/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/malware-signatures/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.GET, "/malware-signatures", "/malware-signatures/increment").hasAnyRole("ADMIN", "USER")
                        .requestMatchers(HttpMethod.POST, "/malware-signatures/by-ids").hasAnyRole("ADMIN", "USER")
                        .requestMatchers("/licenses").hasRole("ADMIN")
                        .requestMatchers("/licenses/**").hasAnyRole("ADMIN", "USER")
                        .anyRequest().authenticated()
                );
        return http.build();
    }

    @Bean
    AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {
        return configuration.getAuthenticationManager();
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
