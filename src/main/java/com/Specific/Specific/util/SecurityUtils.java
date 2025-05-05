package com.Specific.Specific.util;

import com.Specific.Specific.Models.Entities.User;
import com.Specific.Specific.Services.UserService;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * Utility class for security operations
 */
@Component
public class SecurityUtils {
    
    private final UserService userService;
    
    public SecurityUtils(UserService userService) {
        this.userService = userService;
    }
    
    /**
     * Get the currently authenticated user
     * 
     * @return The authenticated user entity
     */
    public User getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String firebaseUid = auth.getName();
        return userService.findUserByFirebaseUid(firebaseUid);
    }
    
    /**
     * Check if the current user owns a resource
     * 
     * @param resourceUserId The user ID of the resource owner
     * @return true if the current user owns the resource, false otherwise
     */
    public boolean isResourceOwner(Long resourceUserId) {
        User currentUser = getCurrentUser();
        return currentUser.getId() == resourceUserId;
    }
} 