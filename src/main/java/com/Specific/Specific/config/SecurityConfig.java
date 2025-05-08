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
     @Autowired
     private FirebaseAuthFilter firebaseAuthFilter;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .addFilterBefore(firebaseAuthFilter, UsernamePasswordAuthenticationFilter.class)
            .authorizeHttpRequests(auth -> {
                // Specific endpoints that are permitted for all users
                auth.requestMatchers("/user/register").permitAll();
                auth.requestMatchers("/user/test").permitAll();
                auth.requestMatchers("/user/test-register").permitAll();
                auth.requestMatchers("/user/firebase-register").permitAll();
                auth.requestMatchers("/translation/**").permitAll();
                auth.requestMatchers("/health").permitAll();
                auth.requestMatchers("/ping").permitAll();
                
                // Endpoints that require authentication
                auth.requestMatchers("/cards/**").authenticated();
                auth.requestMatchers("/user/**").authenticated();
                
                // The "anyRequest" matcher must be the last one
                auth.anyRequest().authenticated();
            });
        
        return http.build();
    }
} 