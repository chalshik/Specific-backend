package com.Specific.Specific.Repository;

import com.Specific.Specific.Models.Entities.Card;
import com.Specific.Specific.Models.Entities.Review;
import com.Specific.Specific.Models.Entities.User;
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
    Optional<Review> findTopByCardAndUserOrderByReviewDateDesc(
            @Param("card") Card card,
            @Param("user") User user
    );

    /**
     * Find reviews that are due for a specific user and deck.
     */
    @Query("SELECT r FROM Review r JOIN r.card c " +
            "WHERE r.user = :user AND c.deck.id = :deckId " +
            "AND DATEADD(DAY, r.interval, r.reviewDate) <= :currentDate")
    List<Review> findDueReviews(
            @Param("user") User user,
            @Param("deckId") Long deckId,
            @Param("currentDate") LocalDateTime currentDate
    );

    /**
     * Find reviews for cards that have high intervals (well-learned).
     */
    @Query("SELECT r FROM Review r WHERE r.user = :user AND r.interval >= :minInterval")
    List<Review> findFinishedReviews(
            @Param("user") User user,
            @Param("minInterval") Integer minInterval
    );
    
    /**
     * Find all reviews for cards from a specific book.
     */
    @Query("SELECT r FROM Review r JOIN r.card c " +
            "WHERE c.book.id = :bookId AND r.user = :user")
    List<Review> findReviewsByBookId(
            @Param("bookId") Long bookId,
            @Param("user") User user
    );
    
    /**
     * Find due reviews for cards from a specific book.
     */
    @Query("SELECT r FROM Review r JOIN r.card c " +
            "WHERE c.book.id = :bookId AND r.user = :user " +
            "AND DATEADD(DAY, r.interval, r.reviewDate) <= :currentDate")
    List<Review> findDueReviewsByBookId(
            @Param("bookId") Long bookId,
            @Param("user") User user,
            @Param("currentDate") LocalDateTime currentDate
    );

    /**
     * Find cards that are due for review for a specific deck.
     * This leverages the relationship directly instead of requiring filtering in the service layer.
     */
    @Query("SELECT DISTINCT r.card FROM Review r " +
            "WHERE r.user = :user AND r.card.deck.id = :deckId " +
            "AND DATEADD(DAY, r.interval, r.reviewDate) <= :currentDate")
    List<Card> findDueCardsForDeck(
            @Param("user") User user,
            @Param("deckId") Long deckId,
            @Param("currentDate") LocalDateTime currentDate
    );
    
    /**
     * Find cards that are due for review for a specific book.
     * This leverages the relationship directly instead of requiring filtering in the service layer.
     */
    @Query("SELECT DISTINCT r.card FROM Review r " +
            "WHERE r.user = :user AND r.card.book.id = :bookId " +
            "AND DATEADD(DAY, r.interval, r.reviewDate) <= :currentDate")
    List<Card> findDueCardsForBook(
            @Param("user") User user,
            @Param("bookId") Long bookId,
            @Param("currentDate") LocalDateTime currentDate
    );
}