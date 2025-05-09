package com.Specific.Specific.config;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

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
import org.springframework.web.util.ContentCachingRequestWrapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
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
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    public FirebaseAuthFilter(Environment environment) {
        this.environment = environment;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        
        // Wrap the request to allow reading the body multiple times
        ContentCachingRequestWrapper wrappedRequest = new ContentCachingRequestWrapper(request);
        
        String path = wrappedRequest.getRequestURI();
        String method = wrappedRequest.getMethod();
        
        logger.debug("Request: {} {}, Security disabled, looking for firebaseUid parameter", 
                method, path);
        
        // Try to get firebaseUid from request parameters, headers, or attributes
        String uid = extractFirebaseUid(wrappedRequest);
        
        // If no firebaseUid found, use default
        if (uid == null || uid.isEmpty()) {
            uid = "auto-authenticated-user";
            logger.debug("No firebaseUid found, using default: {}", uid);
        } else {
            logger.debug("Found firebaseUid in request: {}", uid);
        }
        
        // Store in request attributes for controllers that need access
        wrappedRequest.setAttribute("firebaseUid", uid);
        
        // Create Spring Security Authentication object
        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
            uid,
            null,
            Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER"))
        );
        
        // Set authentication in Security Context
        SecurityContextHolder.getContext().setAuthentication(authentication);
        
        // Continue with the request
        chain.doFilter(wrappedRequest, response);
    }
    
    /**
     * Extract the firebaseUid from various locations in the request:
     * 1. Request body
     * 2. Request parameter
     * 3. Request header
     * 4. Authorization header
     */
    private String extractFirebaseUid(ContentCachingRequestWrapper request) {
        try {
            // First try to get from request body for POST/PUT requests
            if ((request.getMethod().equals("POST") || request.getMethod().equals("PUT")) && 
                request.getContentType() != null && 
                request.getContentType().contains("application/json")) {
                
                // Read the request body
                byte[] bodyBytes = request.getContentAsByteArray();
                if (bodyBytes.length > 0) {
                    String body = new String(bodyBytes, StandardCharsets.UTF_8);
                    JsonNode jsonNode = objectMapper.readTree(body);
                    
                    // Check if firebaseUid is in the JSON body
                    if (jsonNode.has("firebaseUid")) {
                        String uid = jsonNode.get("firebaseUid").asText();
                        if (uid != null && !uid.isEmpty()) {
                            logger.debug("Found firebaseUid in request body: {}", uid);
                            return uid;
                        }
                    }
                }
            }
            
            // Try from request parameter
            String uid = request.getParameter("firebaseUid");
            if (uid != null && !uid.isEmpty()) {
                logger.debug("Found firebaseUid in request parameter: {}", uid);
                return uid;
            }
            
            // Try from header
            uid = request.getHeader("X-Firebase-Uid");
            if (uid != null && !uid.isEmpty()) {
                logger.debug("Found firebaseUid in X-Firebase-Uid header: {}", uid);
                return uid;
            }
            
            // Try from Authorization header without Bearer prefix
            String authHeader = request.getHeader("Authorization");
            if (authHeader != null && !authHeader.startsWith("Bearer ")) {
                logger.debug("Found firebaseUid in Authorization header: {}", authHeader);
                return authHeader;
            }
            
        } catch (Exception e) {
            logger.error("Error extracting firebaseUid from request: {}", e.getMessage(), e);
        }
        
        // If not found, return null
        return null;
    }
}

