package com.Specific.Specific.Controllers;

import com.Specific.Specific.Models.Entities.Review;
import com.Specific.Specific.Models.RequestModels.ReviewRequest;
import com.Specific.Specific.Services.ReviewService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/reviews")
public class ReviewController {
    
    private final ReviewService reviewService;
    
    @Autowired
    public ReviewController(ReviewService reviewService) {
        this.reviewService = reviewService;
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
    public Review submitReview(@Valid @RequestBody ReviewRequest request) {
        return reviewService.processReview(request.getCardId(), request.getRating());
    }
    
    /**
     * Get review statistics for a specific deck
     * This helps users track their progress in learning the deck
     * 
     * @param deckId The deck ID
     * @return Statistics about the deck's reviews
     */
    @GetMapping("/stats/deck/{deckId}")
    public Map<String, Object> getDeckReviewStats(@PathVariable Long deckId) {
        return reviewService.getDeckReviewStatistics(deckId);
    }
    
    /**
     * Get review history for a specific card
     * This allows users to see their progress with a particular card
     * 
     * @param cardId The card ID
     * @return List of reviews for the card
     */
    @GetMapping("/history/card/{cardId}")
    public List<Review> getCardReviewHistory(@PathVariable Long cardId) {
        return reviewService.findReviewsByCard(cardId);
    }
}
