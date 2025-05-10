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
     * Find all reviews for a specific card and user.
     */
    List<Review> findByCardAndUserOrderByReviewDateDesc(
            @Param("card") Card card,
            @Param("user") User user
    );

    /**
     * Find the most recent review for a specific card and user.
     */
    Optional<Review> findTopByCardAndUserOrderByReviewDateDesc(
            @Param("card") Card card,
            @Param("user") User user
    );

    /**
     * Using a native query approach for finding due reviews by deck.
     * This avoids database-specific date arithmetic functions.
     */
    @Query(nativeQuery = true, 
           value = "SELECT r.* FROM review r " +
                  "JOIN card c ON r.card_id = c.id " +
                  "WHERE r.user_id = :userId AND c.deck_id = :deckId " +
                  "AND r.review_date + INTERVAL '1 day' * r.interval <= :currentDate")
    List<Review> findDueReviewsByDeckId(
            @Param("userId") Long userId,
            @Param("deckId") Long deckId,
            @Param("currentDate") LocalDateTime currentDate
    );

    /**
     * Find reviews for cards that have high intervals (well-learned).
     */
    @Query("SELECT r FROM Review r WHERE r.user.id = :userId AND r.interval >= :minInterval")
    List<Review> findFinishedReviewsByUser(
            @Param("userId") Long userId,
            @Param("minInterval") Integer minInterval
    );
    
    /**
     * Find all reviews for cards from a specific book.
     */
    @Query("SELECT r FROM Review r JOIN r.card c " +
            "WHERE c.book.id = :bookId AND r.user.id = :userId")
    List<Review> findReviewsByBookId(
            @Param("bookId") Long bookId,
            @Param("userId") Long userId
    );
    
    /**
     * Using a native query approach for finding due reviews by book.
     * This avoids database-specific date arithmetic functions.
     */
    @Query(nativeQuery = true, 
           value = "SELECT r.* FROM review r " +
                  "JOIN card c ON r.card_id = c.id " +
                  "WHERE c.book_id = :bookId AND r.user_id = :userId " +
                  "AND r.review_date + INTERVAL '1 day' * r.interval <= :currentDate")
    List<Review> findDueReviewsByBookId(
            @Param("bookId") Long bookId,
            @Param("userId") Long userId,
            @Param("currentDate") LocalDateTime currentDate
    );

    /**
     * Using a native query approach for finding due cards by deck.
     * This avoids database-specific date arithmetic functions.
     */
    @Query(nativeQuery = true, 
           value = "SELECT DISTINCT c.* FROM card c " +
                  "JOIN review r ON c.id = r.card_id " +
                  "WHERE r.user_id = :userId AND c.deck_id = :deckId " +
                  "AND r.review_date + INTERVAL '1 day' * r.interval <= :currentDate")
    List<Card> findDueCardsForDeck(
            @Param("userId") Long userId,
            @Param("deckId") Long deckId,
            @Param("currentDate") LocalDateTime currentDate
    );
    
    /**
     * Using a native query approach for finding due cards by book.
     * This avoids database-specific date arithmetic functions.
     */
    @Query(nativeQuery = true, 
           value = "SELECT DISTINCT c.* FROM card c " +
                  "JOIN review r ON c.id = r.card_id " +
                  "WHERE r.user_id = :userId AND c.book_id = :bookId " +
                  "AND r.review_date + INTERVAL '1 day' * r.interval <= :currentDate")
    List<Card> findDueCardsForBook(
            @Param("userId") Long userId,
            @Param("bookId") Long bookId,
            @Param("currentDate") LocalDateTime currentDate
    );
}