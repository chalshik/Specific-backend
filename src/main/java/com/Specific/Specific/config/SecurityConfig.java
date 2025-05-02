package com.Specific.Specific.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
public class SecurityConfig {
    
    // Temporarily commented out for testing
    // @Autowired
    // private FirebaseAuthFilter firebaseAuthFilter;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            // Temporarily commented out Firebase filter for testing
            // .addFilterBefore(firebaseAuthFilter, UsernamePasswordAuthenticationFilter.class)
            .authorizeHttpRequests(auth -> {
                // TEMPORARY: Allow all requests without authentication for testing
                auth.anyRequest().permitAll();
                
                /* Original configuration, temporarily commented out
                // Public endpoints that don't require authentication
                auth.requestMatchers("/user/register").permitAll();
                auth.requestMatchers("/translation/**").permitAll();
                
                // Protected endpoints that require authentication
                auth.requestMatchers("/cards/**").authenticated();
                auth.requestMatchers("/user/**").authenticated();
                
                // Default rule for any other endpoints
                auth.anyRequest().authenticated();
                */
            });
        
        return http.build();
    }
} 