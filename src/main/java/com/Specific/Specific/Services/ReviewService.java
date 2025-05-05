package com.Specific.Specific.Services;

import com.Specific.Specific.Except.CardNotFoundException;
import com.Specific.Specific.Except.InvalidReviewRatingException;
import com.Specific.Specific.Except.ReviewNotFoundException;
import com.Specific.Specific.Models.Entities.Card;
import com.Specific.Specific.Models.Entities.Review;
import com.Specific.Specific.Models.Entities.User;
import com.Specific.Specific.Repository.CardRepo;
import com.Specific.Specific.Repository.ReviewRepo;
import com.Specific.Specific.util.SecurityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 * Service responsible for processing card reviews using the SM-2 spaced repetition algorithm.
 * Handles calculating intervals, ease factors, and repetition counts for flashcards.
 */
@Service
public class ReviewService {
    private final ReviewRepo reviewRepo;
    private final CardRepo cardRepo;
    private final SecurityUtils securityUtils;
    private final AuthorizationService authorizationService;
    
    // TEMPORARY: For testing only
    private static final Long TEST_USER_ID = 1L;
    
    private static final List<String> VALID_RATINGS = Arrays.asList("again", "hard", "good", "easy");
    
    @Autowired
    public ReviewService(
            ReviewRepo reviewRepo, 
            CardRepo cardRepo,
            SecurityUtils securityUtils,
            AuthorizationService authorizationService) {
        this.reviewRepo = reviewRepo;
        this.cardRepo = cardRepo;
        this.securityUtils = securityUtils;
        this.authorizationService = authorizationService;
    }
    
    /**
     * Process a review of a card using the SM-2 algorithm.
     * 
     * @param cardId Card being reviewed
     * @param rating User rating (again, hard, good, easy)
     * @return The new saved Review entity
     */
    public Review processReview(Long cardId, String rating) {
        // Validate rating
        if (!VALID_RATINGS.contains(rating.toLowerCase())) {
            throw new InvalidReviewRatingException("Invalid rating: " + rating + ". Must be one of: again, hard, good, easy");
        }
        
        // Get current user
        User currentUser = securityUtils.getCurrentUser();
        
        // Find the card
        Card card = cardRepo.findById(cardId)
                .orElseThrow(() -> new CardNotFoundException("Card with ID " + cardId + " not found"));
        
        // TEMPORARY: Skip card ownership verification
        // authorizationService.verifyCardAccess(card);
        
        // Current review time
        LocalDateTime reviewDate = LocalDateTime.now();
        
        // Fetch latest review for card-user pair, or initialize for new card
        Optional<Review> latestReviewOpt = reviewRepo.findTopByCardAndUserOrderByReviewDateDesc(card, currentUser);
        
        Review latestReview;
        if (latestReviewOpt.isPresent()) {
            latestReview = latestReviewOpt.get();
        } else {
            // Default values for a new card (first review)
            latestReview = new Review();
            latestReview.setUser(currentUser);
            latestReview.setCard(card);
            latestReview.setReviewDate(reviewDate);
            latestReview.setEaseFactor(2.5);  // Default ease factor in SM-2
            latestReview.setInterval(0);      // Start with 0 day interval
            latestReview.setRepetitions(0);   // No prior repetitions
        }

        // Initialize new review entity
        Review newReview = new Review();
        newReview.setUser(currentUser);
        newReview.setCard(card);
        newReview.setReviewDate(reviewDate);

        // SM-2 calculations based on user rating
        int newInterval;
        double newEaseFactor = latestReview.getEaseFactor();
        int newRepetitions = latestReview.getRepetitions();
        Review.Rating newRating = null;

        switch (rating.toLowerCase()) {
            case "again":
                // Complete failure - reset interval and reduce ease factor
                newInterval = 0;
                newEaseFactor = Math.max(1.3, newEaseFactor - 0.2);
                newRepetitions = 0;
                newRating = Review.Rating.AGAIN;
                break;
            case "hard":
                // Difficult recall - small interval increase and reduce ease
                newInterval = (int) Math.max(1, latestReview.getInterval() * 1.2);
                newEaseFactor = Math.max(1.3, newEaseFactor - 0.15);
                newRepetitions++;
                newRating = Review.Rating.HARD;
                break;
            case "good":
                // Successful recall - standard interval increase
                newInterval = latestReview.getInterval() == 0 ? 1 : (int) (latestReview.getInterval() * newEaseFactor);
                newRepetitions++;
                newRating = Review.Rating.GOOD;
                break;
            case "easy":
                // Perfect recall - larger interval increase and boost ease
                newInterval = latestReview.getInterval() == 0 ? 1 : (int) (latestReview.getInterval() * newEaseFactor * 1.3);
                newEaseFactor = Math.min(2.5, newEaseFactor + 0.1);
                newRepetitions++;
                newRating = Review.Rating.EASY;
                break;
            default:
                // This should never happen due to the validation above
                throw new InvalidReviewRatingException("Invalid rating: " + rating);
        }

        // Update the review with calculated values
        newReview.setInterval(newInterval);
        newReview.setEaseFactor(newEaseFactor);
        newReview.setRepetitions(newRepetitions);
        newReview.setLastResult(newRating);

        // Save the review and update relationships
        currentUser.addReview(newReview);
        card.addReview(newReview);

        // Save to database and return
        return reviewRepo.save(newReview);
    }
    
    /**
     * Find a review by ID
     * 
     * @param reviewId Review ID
     * @return The found review
     * @throws ReviewNotFoundException if review not found
     */
    public Review findReviewById(Long reviewId) {
        // TEMPORARY: Skip ownership verification
        return reviewRepo.findById(reviewId)
                .orElseThrow(() -> new ReviewNotFoundException("Review with ID " + reviewId + " not found"));
    }
    
    /**
     * Find due reviews for a specific deck
     * 
     * @param deckId Deck ID
     * @return List of due reviews
     */
    public List<Review> findDueReviewsByDeck(Long deckId) {
        User currentUser = securityUtils.getCurrentUser();
        return reviewRepo.findDueReviews(currentUser, deckId, LocalDateTime.now());
    }
    
    /**
     * Find due reviews for a specific book
     * 
     * @param bookId Book ID
     * @return List of due reviews
     */
    public List<Review> findDueReviewsByBook(Long bookId) {
        User currentUser = securityUtils.getCurrentUser();
        return reviewRepo.findDueReviewsByBookId(bookId, currentUser, LocalDateTime.now());
    }
    
    /**
     * Find all reviews for a specific card
     * 
     * @param cardId Card ID
     * @return List of reviews for the card
     */
    public List<Review> findReviewsByCard(Long cardId) {
        User currentUser = securityUtils.getCurrentUser();
        
        // Find the card
        Card card = cardRepo.findById(cardId)
                .orElseThrow(() -> new CardNotFoundException("Card with ID " + cardId + " not found"));
        
        return reviewRepo.findTopByCardAndUserOrderByReviewDateDesc(card, currentUser)
                .map(List::of)
                .orElse(List.of());
    }
    
    /**
     * Find cards that are due for review in a specific deck
     * This optimized method directly returns the cards without filtering
     * 
     * @param deckId Deck ID
     * @return List of due cards
     */
    public List<Card> findDueCardsForDeck(Long deckId) {
        User currentUser = securityUtils.getCurrentUser();
        return reviewRepo.findDueCardsForDeck(currentUser, deckId, LocalDateTime.now());
    }
    
    /**
     * Find cards that are due for review in a specific book
     * This optimized method directly returns the cards without filtering
     * 
     * @param bookId Book ID
     * @return List of due cards
     */
    public List<Card> findDueCardsForBook(Long bookId) {
        User currentUser = securityUtils.getCurrentUser();
        return reviewRepo.findDueCardsForBook(currentUser, bookId, LocalDateTime.now());
    }
}
