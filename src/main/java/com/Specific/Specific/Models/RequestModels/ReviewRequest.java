package com.Specific.Specific.Models.RequestModels;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

/**
 * DTO for review creation/update requests
 */
public class ReviewRequest {
    
    @NotNull(message = "Card ID is required")
    private Long cardId;
    
    @NotNull(message = "Rating is required")
    @Pattern(regexp = "(?i)^(again|hard|good|easy)$", message = "Rating must be one of: again, hard, good, easy")
    private String rating;
    
    // Constructors
    public ReviewRequest() {
    }
    
    public ReviewRequest(Long cardId, String rating) {
        this.cardId = cardId;
        this.rating = rating;
    }
    
    // Getters and Setters
    public Long getCardId() {
        return cardId;
    }
    
    public void setCardId(Long cardId) {
        this.cardId = cardId;
    }
    
    public String getRating() {
        return rating;
    }
    
    public void setRating(String rating) {
        this.rating = rating;
    }
} 