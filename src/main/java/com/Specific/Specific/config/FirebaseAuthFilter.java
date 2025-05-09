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
    
    // Always disable Firebase authentication
    private boolean firebaseEnabled = false;
    
    private final Environment environment;
    
    public FirebaseAuthFilter(Environment environment) {
        this.environment = environment;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        
        String path = request.getRequestURI();
        String method = request.getMethod();
        
        logger.debug("Request: {} {}, Security disabled, looking for firebaseUid parameter", 
                method, path);
        
        // Try to get firebaseUid from request parameters, headers, or attributes
        String uid = extractFirebaseUid(request);
        
        // If no firebaseUid found, use default
        if (uid == null || uid.isEmpty()) {
            uid = "auto-authenticated-user";
            logger.debug("No firebaseUid found, using default: {}", uid);
        } else {
            logger.debug("Found firebaseUid in request: {}", uid);
        }
        
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
        
        // Continue with the request
        chain.doFilter(request, response);
    }
    
    /**
     * Extract the firebaseUid from various locations in the request
     * - Request parameter
     * - Request header
     * - Request attribute
     * - Request body (if applicable)
     */
    private String extractFirebaseUid(HttpServletRequest request) {
        // Try from request parameter
        String uid = request.getParameter("firebaseUid");
        if (uid != null && !uid.isEmpty()) {
            return uid;
        }
        
        // Try from header
        uid = request.getHeader("X-Firebase-Uid");
        if (uid != null && !uid.isEmpty()) {
            return uid;
        }
        
        // Try from Authorization header without Bearer prefix
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && !authHeader.startsWith("Bearer ")) {
            return authHeader;
        }
        
        // If not found, return null
        return null;
    }
}

