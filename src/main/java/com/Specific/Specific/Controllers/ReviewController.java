package com.Specific.Specific.Controllers;

import com.Specific.Specific.Models.ApiResponse;
import com.Specific.Specific.Models.Review;
import com.Specific.Specific.Models.ReviewRequest;
import com.Specific.Specific.Services.ReviewService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/reviews")
public class ReviewController {
    
    private final ReviewService reviewService;
    
    @Autowired
    public ReviewController(ReviewService reviewService) {
        this.reviewService = reviewService;
    }
    
    /**
     * Process a review for a card (submit a rating)
     * 
     * @param request The review request containing card ID and rating
     * @return The created review
     */
    @PostMapping
    public Review processReview(@Valid @RequestBody ReviewRequest request) {
        return reviewService.processReview(request.getCardId(), request.getRating());
    }
    
    /**
     * Get a specific review by ID
     * 
     * @param reviewId The review ID
     * @return The review
     */
    @GetMapping("/{reviewId}")
    public Review getReview(@PathVariable Long reviewId) {
        return reviewService.findReviewById(reviewId);
    }
    
    /**
     * Get reviews for a specific card
     * 
     * @param cardId The card ID
     * @return List of reviews for the card
     */
    @GetMapping("/card/{cardId}")
    public List<Review> getReviewsByCard(@PathVariable Long cardId) {
        return reviewService.findReviewsByCard(cardId);
    }
    
    /**
     * Get due reviews for a specific deck
     * 
     * @param deckId The deck ID
     * @return List of due reviews
     */
    @GetMapping("/due/deck/{deckId}")
    public List<Review> getDueReviewsByDeck(@PathVariable Long deckId) {
        return reviewService.findDueReviewsByDeck(deckId);
    }
    
    /**
     * Get due reviews for a specific book
     * 
     * @param bookId The book ID
     * @return List of due reviews
     */
    @GetMapping("/due/book/{bookId}")
    public List<Review> getDueReviewsByBook(@PathVariable Long bookId) {
        return reviewService.findDueReviewsByBook(bookId);
    }
}
