package com.Specific.Specific.Controllers;

import com.Specific.Specific.Models.Card;
import com.Specific.Specific.Services.CardService;
import com.Specific.Specific.Services.ReviewService;
import com.Specific.Specific.Models.Review;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/cards")
public class CardController {
    
    private final CardService cardService;
    private final ReviewService reviewService;
    
    @Autowired
    public CardController(CardService cardService, ReviewService reviewService) {
        this.cardService = cardService;
        this.reviewService = reviewService;
    }
    
    /**
     * Get all cards in a deck
     */
    @GetMapping("/deck/{deckId}")
    public List<Card> getCardsByDeck(@PathVariable Long deckId) {
        return cardService.getCardsByDeck(deckId);
    }
    
    /**
     * Get cards that are due for review in a deck
     */
    @GetMapping("/due/deck/{deckId}")
    public List<Card> getDueCardsByDeck(@PathVariable Long deckId) {
        // Get due reviews for this deck
        List<Review> dueReviews = reviewService.findDueReviewsByDeck(deckId);
        
        // Extract card IDs from reviews
        List<Long> cardIds = dueReviews.stream()
                .map(Review::getCardId)
                .collect(Collectors.toList());
        
        // If there are no due cards, return empty list
        if (cardIds.isEmpty()) {
            return new ArrayList<>();
        }
        
        // Get all cards in the deck
        List<Card> deckCards = cardService.getCardsByDeck(deckId);
        
        // Filter cards to only include those that are due
        return deckCards.stream()
                .filter(card -> cardIds.contains(card.getId()))
                .collect(Collectors.toList());
    }
    
    /**
     * Get cards that are due for review in a book
     */
    @GetMapping("/due/book/{bookId}")
    public List<Card> getDueCardsByBook(@PathVariable Long bookId) {
        // Get due reviews for this book
        List<Review> dueReviews = reviewService.findDueReviewsByBook(bookId);
        
        // Extract card IDs from reviews
        List<Long> cardIds = dueReviews.stream()
                .map(Review::getCardId)
                .collect(Collectors.toList());
        
        // If there are no due cards, return empty list
        if (cardIds.isEmpty()) {
            return new ArrayList<>();
        }
        
        // Get all cards in the book
        List<Card> bookCards = cardService.getCardsByBook(bookId);
        
        // Filter cards to only include those that are due
        return bookCards.stream()
                .filter(card -> cardIds.contains(card.getId()))
                .collect(Collectors.toList());
    }
} 