package com.Specific.Specific.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
     private static final Logger logger = LoggerFactory.getLogger(SecurityConfig.class);
     
     @Autowired
     private FirebaseAuthFilter firebaseAuthFilter;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        logger.info("Configuring security settings");
        http
            .csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .addFilterBefore(firebaseAuthFilter, UsernamePasswordAuthenticationFilter.class)
            .authorizeHttpRequests(auth -> {
                // Specific endpoints that are permitted for all users
                auth.requestMatchers("/").permitAll();
                auth.requestMatchers("/user/register").permitAll();
                auth.requestMatchers("/user/debug-register").permitAll();
                auth.requestMatchers("/user/test").permitAll();
                auth.requestMatchers("/user/test-register").permitAll();
                auth.requestMatchers("/user/debug-test-register").permitAll();
                auth.requestMatchers("/user/direct-register").permitAll();
                auth.requestMatchers("/user/db-test").permitAll();
                auth.requestMatchers("/user/auth-test").permitAll();
                auth.requestMatchers("/user/firebase-verify").permitAll();
                auth.requestMatchers("/user/bypass-auth/**").permitAll();
                auth.requestMatchers("/health").permitAll();
                auth.requestMatchers("/translation/**").permitAll();
                
                // Endpoints that require authentication
                auth.requestMatchers("/cards/**").authenticated();
                auth.requestMatchers("/user/**").authenticated();
                
                // The "anyRequest" matcher must be the last one
                auth.anyRequest().authenticated();
            });
        
        logger.info("Security configuration completed");
        return http.build();
    }
} 