package com.Specific.Specific.Services;

import com.Specific.Specific.Except.CardNotFoundException;
import com.Specific.Specific.Except.InvalidReviewRatingException;
import com.Specific.Specific.Except.ReviewNotFoundException;
import com.Specific.Specific.Models.Entities.Card;
import com.Specific.Specific.Models.Entities.Review;
import com.Specific.Specific.Models.Entities.User;
import com.Specific.Specific.Repository.CardRepo;
import com.Specific.Specific.Repository.ReviewRepo;
import com.Specific.Specific.utils.SecurityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Service responsible for processing card reviews using the SM-2 spaced repetition algorithm.
 * Handles calculating intervals, ease factors, and repetition counts for flashcards.
 */
@Service
public class ReviewService {
    private static final Logger logger = LoggerFactory.getLogger(ReviewService.class);
    
    private final ReviewRepo reviewRepo;
    private final CardRepo cardRepo;
    private final SecurityUtils securityUtils;
    private final AuthorizationService authorizationService;
    private final CardService cardService;
    
    private static final List<String> VALID_RATINGS = Arrays.asList("again", "hard", "good", "easy");
    
    @Autowired
    public ReviewService(
            ReviewRepo reviewRepo, 
            CardRepo cardRepo,
            SecurityUtils securityUtils,
            AuthorizationService authorizationService,
            CardService cardService) {
        this.reviewRepo = reviewRepo;
        this.cardRepo = cardRepo;
        this.securityUtils = securityUtils;
        this.authorizationService = authorizationService;
        this.cardService = cardService;
    }
    
    /**
     * Create a new Review entity with proper associations and default values
     * 
     * @param user The user who performed the review
     * @param card The card being reviewed
     * @param reviewDate The date/time of the review
     * @param easeFactor The calculated ease factor
     * @param interval The calculated interval
     * @param repetitions The number of repetitions
     * @param rating The rating given by the user
     * @return A new Review entity with all fields populated
     */
    private Review createReview(User user, Card card, LocalDateTime reviewDate, 
                               Double easeFactor, Integer interval, Integer repetitions, 
                               Review.Rating rating) {
        Review review = new Review();
        review.setUser(user);
        review.setCard(card);
        review.setReviewDate(reviewDate);
        review.setEaseFactor(easeFactor);
        review.setInterval(interval);
        review.setRepetitions(repetitions);
        review.setLastResult(rating);
        
        // Update relationships
        user.addReview(review);
        card.addReview(review);
        
        return review;
    }
    
    /**
     * Process a review of a card using the SM-2 algorithm.
     * 
     * @param cardId Card being reviewed
     * @param rating User rating (again, hard, good, easy)
     * @return The new saved Review entity
     */
    public Review processReview(Long cardId, String rating) {
        // Get current user
        User currentUser = securityUtils.getCurrentUser();
        return processReview(cardId, rating, currentUser);
    }
    
    /**
     * Process a review of a card using the SM-2 algorithm with a specific user.
     * 
     * @param cardId Card being reviewed
     * @param rating User rating (again, hard, good, easy)
     * @param user The user performing the review
     * @return The new saved Review entity
     */
    public Review processReview(Long cardId, String rating, User user) {
        try {
            // Validate rating
            if (!VALID_RATINGS.contains(rating.toLowerCase())) {
                throw new InvalidReviewRatingException("Invalid rating: " + rating + ". Must be one of: again, hard, good, easy");
            }
            
            // Find the card
            Card card = cardRepo.findById(cardId)
                    .orElseThrow(() -> new CardNotFoundException("Card with ID " + cardId + " not found"));
            
            // TEMPORARY: Skip card ownership verification
            // authorizationService.verifyCardAccess(card);
            
            // Current review time
            LocalDateTime reviewDate = LocalDateTime.now();
            
            // Fetch latest review for card-user pair, or initialize for new card
            Optional<Review> latestReviewOpt = reviewRepo.findTopByCardAndUserOrderByReviewDateDesc(card, user);
            
            // SM-2 calculations based on user rating
            int newInterval;
            double newEaseFactor;
            int newRepetitions;
            Review.Rating newRating = null;
            
            if (latestReviewOpt.isPresent()) {
                // This is a follow-up review
                Review latestReview = latestReviewOpt.get();
                
                newEaseFactor = latestReview.getEaseFactor();
                newRepetitions = latestReview.getRepetitions();
                
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
            } else {
                // This is the first review for this card
                newEaseFactor = 2.5; // Default ease factor in SM-2
                newRepetitions = 1;  // First repetition
                
                switch (rating.toLowerCase()) {
                    case "again":
                        newInterval = 0;
                        newRating = Review.Rating.AGAIN;
                        break;
                    case "hard":
                        newInterval = 1;
                        newRating = Review.Rating.HARD;
                        break;
                    case "good":
                        newInterval = 1;
                        newRating = Review.Rating.GOOD;
                        break;
                    case "easy":
                        newInterval = 2;
                        newRating = Review.Rating.EASY;
                        break;
                    default:
                        throw new InvalidReviewRatingException("Invalid rating: " + rating);
                }
            }

            // Create the new review
            Review newReview = new Review();
            newReview.setUser(user);
            newReview.setCard(card);
            newReview.setReviewDate(reviewDate);
            newReview.setEaseFactor(newEaseFactor);
            newReview.setInterval(newInterval);
            newReview.setRepetitions(newRepetitions);
            newReview.setLastResult(newRating);

            // Save to database and return
            return reviewRepo.save(newReview);
        } catch (Exception e) {
            // Log the exception
            logger.error("Error processing review for card ID {}: {}", cardId, e.getMessage(), e);
            throw e;
        }
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
        return findDueReviewsByDeck(deckId, currentUser);
    }
    
    /**
     * Find due reviews for a specific deck for a specific user
     * 
     * @param deckId Deck ID
     * @param user The user who owns the reviews
     * @return List of due reviews
     */
    public List<Review> findDueReviewsByDeck(Long deckId, User user) {
        return reviewRepo.findDueReviewsByDeckId(user, deckId, LocalDateTime.now());
    }
    
    /**
     * Find due reviews for a specific book
     * 
     * @param bookId Book ID
     * @return List of due reviews
     */
    public List<Review> findDueReviewsByBook(Long bookId) {
        User currentUser = securityUtils.getCurrentUser();
        return findDueReviewsByBook(bookId, currentUser);
    }
    
    /**
     * Find due reviews for a specific book for a specific user
     * 
     * @param bookId Book ID
     * @param user The user who owns the reviews
     * @return List of due reviews
     */
    public List<Review> findDueReviewsByBook(Long bookId, User user) {
        return reviewRepo.findDueReviewsByBookId(bookId, user, LocalDateTime.now());
    }
    
    /**
     * Find all reviews for a specific card
     * 
     * @param cardId Card ID
     * @return List of reviews for the card
     */
    public List<Review> findReviewsByCard(Long cardId) {
        User currentUser = securityUtils.getCurrentUser();
        return findReviewsByCard(cardId, currentUser);
    }
    
    /**
     * Find all reviews for a specific card for a specific user
     * 
     * @param cardId Card ID
     * @param user The user who owns the reviews
     * @return List of reviews for the card
     */
    public List<Review> findReviewsByCard(Long cardId, User user) {
        // Find the card
        Card card = cardRepo.findById(cardId)
                .orElseThrow(() -> new CardNotFoundException("Card with ID " + cardId + " not found"));
        
        // Get all reviews for this card-user pair, not just the latest one
        return reviewRepo.findByCardAndUserOrderByReviewDateDesc(card, user);
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
        return findDueCardsForDeck(deckId, currentUser);
    }
    
    /**
     * Find cards that are due for review in a specific deck for a specific user
     * This optimized method directly returns the cards without filtering
     * 
     * @param deckId Deck ID
     * @param user The user who owns the cards
     * @return List of due cards
     */
    public List<Card> findDueCardsForDeck(Long deckId, User user) {
        return reviewRepo.findDueCardsForDeck(user, deckId, LocalDateTime.now());
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
        return findDueCardsForBook(bookId, currentUser);
    }
    
    /**
     * Find cards that are due for review in a specific book for a specific user
     * This optimized method directly returns the cards without filtering
     * 
     * @param bookId Book ID
     * @param user The user who owns the cards
     * @return List of due cards
     */
    public List<Card> findDueCardsForBook(Long bookId, User user) {
        return reviewRepo.findDueCardsForBook(user, bookId, LocalDateTime.now());
    }
    
    /**
     * Find all reviews for cards from a specific book
     * 
     * @param bookId Book ID
     * @return List of reviews
     */
    public List<Review> findReviewsByBook(Long bookId) {
        User currentUser = securityUtils.getCurrentUser();
        return findReviewsByBook(bookId, currentUser);
    }
    
    /**
     * Find all reviews for cards from a specific book for a specific user
     * 
     * @param bookId Book ID
     * @param user The user who owns the reviews
     * @return List of reviews
     */
    public List<Review> findReviewsByBook(Long bookId, User user) {
        return reviewRepo.findReviewsByBookId(bookId, user);
    }

    /**
     * Get review statistics for a specific deck
     * 
     * @param deckId Deck ID
     * @return Map containing statistics about the deck's reviews
     */
    public Map<String, Object> getDeckReviewStatistics(Long deckId) {
        User currentUser = securityUtils.getCurrentUser();
        return getDeckReviewStatistics(deckId, currentUser);
    }
    
    /**
     * Get review statistics for a specific deck for a specific user
     * 
     * @param deckId Deck ID
     * @param user The user who owns the reviews
     * @return Map containing statistics about the deck's reviews
     */
    public Map<String, Object> getDeckReviewStatistics(Long deckId, User user) {
        LocalDateTime now = LocalDateTime.now();
        
        // Get all reviews for this deck's cards
        List<Review> allReviews = reviewRepo.findDueReviewsByDeckId(user, deckId, now);
        
        // Get cards due for review
        List<Card> dueCards = findDueCardsForDeck(deckId, user);
        
        // Get all cards in the deck (via service to enforce access control)
        List<Card> allCards = cardService.getCardsByDeck(deckId, user);
        
        // Calculate statistics
        int totalCards = allCards.size();
        int dueCardsCount = dueCards.size();
        int reviewedCardsCount = totalCards - dueCardsCount;
        
        // Count reviews by result
        long againCount = allReviews.stream()
            .filter(r -> r.getLastResult() == Review.Rating.AGAIN)
            .count();
        
        long hardCount = allReviews.stream()
            .filter(r -> r.getLastResult() == Review.Rating.HARD)
            .count();
        
        long goodCount = allReviews.stream()
            .filter(r -> r.getLastResult() == Review.Rating.GOOD)
            .count();
        
        long easyCount = allReviews.stream()
            .filter(r -> r.getLastResult() == Review.Rating.EASY)
            .count();
        
        // Calculate performance metrics
        double totalReviews = againCount + hardCount + goodCount + easyCount;
        double successRate = totalReviews > 0 ? 
                ((goodCount + easyCount) / totalReviews) * 100.0 : 0.0;
        
        // Calculate average interval
        double avgInterval = allReviews.stream()
            .mapToInt(Review::getInterval)
            .average()
            .orElse(0.0);
        
        // Compile all statistics
        Map<String, Object> statistics = new HashMap<>();
        statistics.put("totalCards", totalCards);
        statistics.put("dueCards", dueCardsCount);
        statistics.put("reviewedCards", reviewedCardsCount);
        statistics.put("againCount", againCount);
        statistics.put("hardCount", hardCount);
        statistics.put("goodCount", goodCount);
        statistics.put("easyCount", easyCount);
        statistics.put("successRate", successRate);
        statistics.put("averageInterval", avgInterval);
        
        return statistics;
    }
}
