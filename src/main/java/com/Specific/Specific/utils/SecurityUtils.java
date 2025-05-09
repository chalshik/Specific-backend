package com.Specific.Specific.utils;

import com.Specific.Specific.Except.UserNotFoundException;
import com.Specific.Specific.Models.Entities.User;
import com.Specific.Specific.Services.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;

@Component
public class SecurityUtils {
    private static final Logger logger = LoggerFactory.getLogger(SecurityUtils.class);
    
    private final UserService userService;
    
    @Autowired
    public SecurityUtils(UserService userService) {
        this.userService = userService;
    }

    /**
     * Get the current user's Firebase UID.
     * Will prioritize firebaseUid from request parameters if available.
     */
    public String getCurrentUserFirebaseUid() {
        // First check if we have a firebaseUid parameter in the current request
        String firebaseUid = getFirebaseUidFromRequest();
        if (firebaseUid != null && !firebaseUid.isEmpty()) {
            logger.debug("Using firebaseUid from request: {}", firebaseUid);
            return firebaseUid;
        }
        
        // Then check authentication context
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() != null) {
            firebaseUid = authentication.getPrincipal().toString();
            logger.debug("Using firebaseUid from Security Context: {}", firebaseUid);
            return firebaseUid;
        }
        
        // Fallback to default
        logger.debug("No firebaseUid found, using default");
        return "auto-authenticated-user";
    }
    
    /**
     * Get the current user entity from the database
     * @return The current user
     * @throws UserNotFoundException if the user with the current Firebase UID is not found
     */
    public User getCurrentUser() {
        String firebaseUid = getCurrentUserFirebaseUid();
        try {
            logger.debug("Looking up user with firebaseUid: {}", firebaseUid);
            return userService.findUserByFirebaseUid(firebaseUid);
        } catch (UserNotFoundException e) {
            logger.error("User not found with firebaseUid: {}", firebaseUid);
            throw e;
        }
    }
    
    /**
     * Check if the current user is the owner of a resource
     * 
     * @param resourceUserId The ID of the user who owns the resource
     * @return true if the current user is the owner, false otherwise
     */
    public boolean isResourceOwner(Long resourceUserId) {
        try {
            User currentUser = getCurrentUser();
            boolean isOwner = currentUser.getId() == resourceUserId;
            logger.debug("Checking resource ownership: currentUser={}, resourceOwner={}, isOwner={}", 
                    currentUser.getId(), resourceUserId, isOwner);
            return isOwner;
        } catch (Exception e) {
            logger.error("Error checking resource ownership: {}", e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * Try to get firebaseUid from the current request attributes
     */
    private String getFirebaseUidFromRequest() {
        try {
            ServletRequestAttributes requestAttributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (requestAttributes != null) {
                HttpServletRequest request = requestAttributes.getRequest();
                
                // First try from the request attribute (set by FirebaseAuthFilter)
                Object uidAttr = request.getAttribute("firebaseUid");
                if (uidAttr != null) {
                    String uid = uidAttr.toString();
                    if (!uid.isEmpty()) {
                        return uid;
                    }
                }
                
                // Then try from the request parameter
                String uidParam = request.getParameter("firebaseUid");
                if (uidParam != null && !uidParam.isEmpty()) {
                    return uidParam;
                }
            }
        } catch (Exception e) {
            logger.error("Error getting firebaseUid from request: {}", e.getMessage(), e);
        }
        return null;
    }
} 