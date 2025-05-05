package com.Specific.Specific.Services;

import com.Specific.Specific.Except.UnauthorizedAccessException;
import com.Specific.Specific.Models.Entities.Deck;
import com.Specific.Specific.util.SecurityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Service for handling authorization checks
 */
@Service
public class AuthorizationService {
    
    private final SecurityUtils securityUtils;
    
    @Autowired
    public AuthorizationService(SecurityUtils securityUtils) {
        this.securityUtils = securityUtils;
    }
    
    /**
     * Verify that the current user is the owner of the deck
     * 
     * @param deck The deck to check
     * @throws UnauthorizedAccessException if the user is not the owner
     */
    public void verifyDeckOwner(Deck deck) {
        if (!securityUtils.isResourceOwner(deck.getUser().getId())) {
            throw new UnauthorizedAccessException("You don't have permission to access this deck");
        }
    }
    
    /**
     * Verify that the current user is the owner of the resource
     * 
     * @param resourceUserId The user ID of the resource owner
     * @throws UnauthorizedAccessException if the user is not the owner
     */
    public void verifyResourceOwner(Long resourceUserId) {
        if (!securityUtils.isResourceOwner(resourceUserId)) {
            throw new UnauthorizedAccessException("You don't have permission to access this resource");
        }
    }
} 