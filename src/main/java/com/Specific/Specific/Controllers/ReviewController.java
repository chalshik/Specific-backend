package com.Specific.Specific.Controllers;

import com.Specific.Specific.Models.Entities.Review;
import com.Specific.Specific.Models.Entities.User;
import com.Specific.Specific.Models.RequestModels.ReviewRequest;
import com.Specific.Specific.Models.ResponseModels.ApiResponse;
import com.Specific.Specific.Services.ReviewService;
import com.Specific.Specific.Services.UserService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/reviews")
public class ReviewController {
    
    private static final Logger logger = LoggerFactory.getLogger(ReviewController.class);
    private final ReviewService reviewService;
    private final UserService userService;
    
    @Autowired
    public ReviewController(ReviewService reviewService, UserService userService) {
        this.reviewService = reviewService;
        this.userService = userService;
    }
    
    /**
     * Submit a review for a card (rating: again, hard, good, easy)
     * This is the core functionality of the spaced repetition system
     * The algorithm will calculate the next review date based on the rating
     * 
     * @param request The review request containing card ID and rating
     * @return The created review with the calculated next interval
     */
    @PostMapping
    public Review submitReview(
            @Valid @RequestBody ReviewRequest request,
            @RequestParam(required = false) String firebaseUid) {
        // Extract Firebase UID from request or parameter
        String uid = request.getFirebaseUid() != null ? request.getFirebaseUid() : 
                    (firebaseUid != null ? firebaseUid : "auto-authenticated-user");
        
        logger.info("Submitting review for card ID: {}, rating: {}, firebaseUid: {}", 
                  request.getCardId(), request.getRating(), uid);
        
        // Get user directly
        User user = userService.findUserByFirebaseUid(uid);
        
        // Process review with user object
        return reviewService.processReview(request.getCardId(), request.getRating(), user);
    }
    
    /**
     * Get review statistics for a specific deck
     * This helps users track their progress in learning the deck
     * 
     * @param deckId The deck ID
     * @return Statistics about the deck's reviews
     */
    @GetMapping("/stats/deck/{deckId}")
    public Map<String, Object> getDeckReviewStatistics(
            @PathVariable Long deckId,
            @RequestParam(required = false) String firebaseUid) {
        // Extract Firebase UID from parameter
        String uid = firebaseUid != null ? firebaseUid : "auto-authenticated-user";
        
        logger.info("Getting review statistics for deck ID: {}, firebaseUid: {}", deckId, uid);
        
        // Get user directly
        User user = userService.findUserByFirebaseUid(uid);
        
        // Get statistics with user object
        return reviewService.getDeckReviewStatistics(deckId, user);
    }
    
    /**
     * Get all reviews for a specific card
     * 
     * @param cardId The card ID
     * @return List of reviews for the card
     */
    @GetMapping("/card/{cardId}")
    public List<Review> getCardReviews(
            @PathVariable Long cardId,
            @RequestParam(required = false) String firebaseUid) {
        // Extract Firebase UID from parameter
        String uid = firebaseUid != null ? firebaseUid : "auto-authenticated-user";
        
        logger.info("Getting reviews for card ID: {}, firebaseUid: {}", cardId, uid);
        
        // Get user directly
        User user = userService.findUserByFirebaseUid(uid);
        
        // Get reviews with user object
        return reviewService.findReviewsByCard(cardId, user);
    }
    
    /**
     * Get all reviews for a specific book
     * 
     * @param bookId The book ID
     * @return List of reviews for cards in the book
     */
    @GetMapping("/book/{bookId}")
    public List<Review> getBookReviews(
            @PathVariable Long bookId,
            @RequestParam(required = false) String firebaseUid) {
        // Extract Firebase UID from parameter
        String uid = firebaseUid != null ? firebaseUid : "auto-authenticated-user";
        
        logger.info("Getting reviews for book ID: {}, firebaseUid: {}", bookId, uid);
        
        // Get user directly
        User user = userService.findUserByFirebaseUid(uid);
        
        // Get reviews with user object
        return reviewService.findReviewsByBook(bookId, user);
    }
    
    /**
     * Helper method to extract Firebase UID from various sources
     */
    private String extractFirebaseUid(String paramFirebaseUid, Object requestBody) {
        // First try from request parameter
        if (paramFirebaseUid != null && !paramFirebaseUid.isEmpty()) {
            return paramFirebaseUid;
        }
        
        // Then try from request body if it's a map
        if (requestBody instanceof java.util.Map) {
            Object bodyFirebaseUid = ((java.util.Map<?, ?>) requestBody).get("firebaseUid");
            if (bodyFirebaseUid != null) {
                return bodyFirebaseUid.toString();
            }
        }
        
        // Default value
        return "auto-authenticated-user";
    }
}
