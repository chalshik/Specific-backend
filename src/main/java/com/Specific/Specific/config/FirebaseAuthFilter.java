package com.Specific.Specific.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseToken;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@Component
public class FirebaseAuthFilter extends OncePerRequestFilter {
    private static final Logger logger = LoggerFactory.getLogger(FirebaseAuthFilter.class);
    
    @Value("${firebase.enabled:false}")
    private boolean firebaseEnabled;
    
    private final Environment environment;
    
    public FirebaseAuthFilter(Environment environment) {
        this.environment = environment;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        
        // Skip auth for development profile or if firebase is disabled
        boolean isDevelopment = Arrays.asList(environment.getActiveProfiles()).contains("dev") || 
                                !firebaseEnabled;
        
        String path = request.getRequestURI();
        String method = request.getMethod();
        
        logger.debug("Request: {} {}, Firebase enabled: {}, Development mode: {}", 
                method, path, firebaseEnabled, isDevelopment);
        
        // Skip auth for registration endpoints
        if (path.equals("/user/register") || 
            path.equals("/user/test-register") || 
            path.equals("/user/debug-register") || 
            path.equals("/user/debug-test-register") ||
            path.equals("/health") ||
            path.startsWith("/translation/")) {
            chain.doFilter(request, response);
            return;
        }
        
        String header = request.getHeader("Authorization");

        if (header != null && header.startsWith("Bearer ")) {
            String token = header.replace("Bearer ", "");
            try {
                FirebaseToken decodedToken = FirebaseAuth.getInstance().verifyIdToken(token);
                String uid = decodedToken.getUid();
                
                logger.debug("Authenticated request with Firebase UID: {}", uid);
                
                // Store in request attributes for controllers that need access
                request.setAttribute("firebaseUid", uid);
                
                // Create Spring Security Authentication object
                UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                    uid,
                    null,
                    Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER"))
                );
                
                // Set authentication in Security Context
                SecurityContextHolder.getContext().setAuthentication(authentication);
            } catch (FirebaseAuthException e) {
                // Log error details
                logger.error("Firebase Authentication error: Code: {}, Message: {}, Full Error: {}", 
                            e.getErrorCode(), e.getMessage(), e, e);
                
                // Include more detailed information in the response
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("error", e.getErrorCode());
                errorResponse.put("message", "Authentication failed");
                errorResponse.put("details", e.getMessage());
                
                // Return appropriate HTTP status and error message
                response.setStatus(HttpStatus.UNAUTHORIZED.value());
                response.setContentType("application/json");
                response.getWriter().write(new ObjectMapper().writeValueAsString(errorResponse));
                return;
            } catch (Exception e) {
                // Log general exceptions that might occur during authentication
                logger.error("Unexpected error during authentication: {}", e.getMessage(), e);
                
                // Return appropriate HTTP status and error message
                response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
                response.setContentType("application/json");
                response.getWriter().write("{\"error\":\"INTERNAL_ERROR\",\"message\":\"Authentication failed due to an internal error\"}");
                return;
            }
        } else if (isDevelopment) {
            // For development mode, allow requests without tokens
            logger.debug("Development mode: Proceeding without authentication for {}", path);
            UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                "dev-user",
                null,
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER"))
            );
            SecurityContextHolder.getContext().setAuthentication(authentication);
        } else {
            logger.debug("No authentication token found for {}", path);
        }
        
        chain.doFilter(request, response);
    }
}

