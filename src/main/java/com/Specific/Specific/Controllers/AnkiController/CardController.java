package com.Specific.Specific.Controllers.AnkiController;

import com.Specific.Specific.Models.Entities.Card;
import com.Specific.Specific.Services.CardService;
import com.Specific.Specific.Services.ReviewService;
import com.Specific.Specific.Models.Entities.Review;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
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
        // Directly get due cards using the optimized service method
        return reviewService.findDueCardsForDeck(deckId);
    }
    
    /**
     * Get cards that are due for review in a book
     */
    @GetMapping("/due/book/{bookId}")
    public List<Card> getDueCardsByBook(@PathVariable Long bookId) {
        // Directly get due cards using the optimized service method
        return reviewService.findDueCardsForBook(bookId);
    }
    @PostMapping("/add-card/{deckId}")
    public Card addCard(@RequestBody Card card){
        return cardService.createCard(card);
    }
} 