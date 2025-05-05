package com.Specific.Specific.Repository;

import com.Specific.Specific.Models.Entities.Review;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository for managing Review entities.
 * Provides methods for finding reviews by various criteria.
 */
@Repository
public interface ReviewRepo extends JpaRepository<Review, Long> {

    /**
     * Find the most recent review for a specific card and user.
     */
    Optional<Review> findTopByCardIdAndUserIdOrderByReviewDateDesc(
            @Param("cardId") Long cardId,
            @Param("userId") Long userId
    );

    /**
     * Find reviews that are due for a specific user and deck.
     */
    @Query("SELECT r FROM Review r JOIN Card c ON r.cardId = c.id " +
            "WHERE r.userId = :userId AND c.deckId = :deckId " +
            "AND DATEADD(DAY, r.interval, r.reviewDate) <= :currentDate")
    List<Review> findDueReviews(
            @Param("userId") Long userId,
            @Param("deckId") Long deckId,
            @Param("currentDate") LocalDateTime currentDate
    );

    /**
     * Find reviews for cards that have high intervals (well-learned).
     */
    @Query("SELECT r FROM Review r WHERE r.userId = :userId AND r.interval >= :minInterval")
    List<Review> findFinishedReviews(
            @Param("userId") Long userId,
            @Param("minInterval") Integer minInterval
    );
    
    /**
     * Find all reviews for cards from a specific book.
     */
    @Query("SELECT r FROM Review r JOIN Card c ON r.cardId = c.id " +
            "WHERE c.bookId = :bookId AND r.userId = :userId")
    List<Review> findReviewsByBookId(
            @Param("bookId") Long bookId,
            @Param("userId") Long userId
    );
    
    /**
     * Find due reviews for cards from a specific book.
     */
    @Query("SELECT r FROM Review r JOIN Card c ON r.cardId = c.id " +
            "WHERE c.bookId = :bookId AND r.userId = :userId " +
            "AND DATEADD(DAY, r.interval, r.reviewDate) <= :currentDate")
    List<Review> findDueReviewsByBookId(
            @Param("bookId") Long bookId,
            @Param("userId") Long userId,
            @Param("currentDate") LocalDateTime currentDate
    );
}