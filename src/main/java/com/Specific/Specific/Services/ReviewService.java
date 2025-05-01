package com.Specific.Specific.Services;

import com.Specific.Specific.Models.Review;
import com.Specific.Specific.Repository.ReviewRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Service responsible for processing card reviews using the SM-2 spaced repetition algorithm.
 * Handles calculating intervals, ease factors, and repetition counts for flashcards.
 */
@Service
public class ReviewService {
    private final ReviewRepo reviewRepo;
    
    @Autowired
    public ReviewService(ReviewRepo reviewRepo) {
        this.reviewRepo = reviewRepo;
    }
    
    /**
     * Process a review of a card using the SM-2 algorithm.
     * 
     * @param userId User performing the review
     * @param cardId Card being reviewed
     * @param rating User rating (again, hard, good, easy)
     * @param reviewDate Date/time of the review
     * @return The new saved Review entity
     */
    public Review processReview(long userId, long cardId, String rating, LocalDateTime reviewDate) {
        // Fetch latest review for card-user pair, or initialize for new card
        Optional<Review> latestReviewOpt = reviewRepo.findTopByCardIdAndUserIdOrderByReviewDateDesc(cardId, userId);
        
        Review latestReview;
        if (latestReviewOpt.isPresent()) {
            latestReview = latestReviewOpt.get();
        } else {
            // Default values for a new card (first review)
            latestReview = new Review();
            latestReview.setUserId(userId);
            latestReview.setCardId(cardId);
            latestReview.setReviewDate(reviewDate);
            latestReview.setEaseFactor(2.5);  // Default ease factor in SM-2
            latestReview.setInterval(0);      // Start with 0 day interval
            latestReview.setRepetitions(0);   // No prior repetitions
        }

        // Initialize new review entity
        Review newReview = new Review();
        newReview.setUserId(userId);
        newReview.setCardId(cardId);
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
                throw new IllegalArgumentException("Invalid rating: " + rating);
        }

        // Update the review with calculated values
        newReview.setInterval(newInterval);
        newReview.setEaseFactor(newEaseFactor);
        newReview.setRepetitions(newRepetitions);
        newReview.setLastResult(newRating);

        // Save to database and return
        return reviewRepo.save(newReview);
    }
}
