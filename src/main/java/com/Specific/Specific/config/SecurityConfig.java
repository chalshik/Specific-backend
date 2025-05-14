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
     
     // Commented out Firebase filter as we're disabling security
     // @Autowired
     // private FirebaseAuthFilter firebaseAuthFilter;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        logger.info("Configuring security settings - ALL SECURITY DISABLED");
        http
            .csrf(AbstractHttpConfigurer::disable)
            .cors(cors -> cors.configure(http)) // Explicitly enable CORS
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            // Remove Firebase auth filter
            // .addFilterBefore(firebaseAuthFilter, UsernamePasswordAuthenticationFilter.class)
            .authorizeHttpRequests(auth -> {
                // Explicitly permit WebSocket endpoints
                auth.requestMatchers("/ws-game/**").permitAll();
                auth.requestMatchers("/ws-game").permitAll();
                auth.requestMatchers("/app/**").permitAll();
                auth.requestMatchers("/topic/**").permitAll();
                auth.requestMatchers("/queue/**").permitAll();
                // Permit ALL other requests without authentication
                auth.anyRequest().permitAll();
            });
        
        logger.info("Security configuration completed - ALL ENDPOINTS ARE PUBLIC WITH CORS ENABLED");
        return http.build();
    }
} 