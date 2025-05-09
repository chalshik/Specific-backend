package com.Specific.Specific.Controllers.AnkiController;

import com.Specific.Specific.Models.Entities.Card;
import com.Specific.Specific.Services.CardService;
import com.Specific.Specific.Services.ReviewService;
import com.Specific.Specific.Services.DeckService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/cards")
public class CardController {
    
    private final CardService cardService;
    private final ReviewService reviewService;
    private final DeckService deckService;
    
    @Autowired
    public CardController(CardService cardService, ReviewService reviewService, DeckService deckService) {
        this.cardService = cardService;
        this.reviewService = reviewService;
        this.deckService = deckService;
    }
    
    /**
     * Get cards that are due for review in a deck
     * This is the primary endpoint users should use to study their cards,
     * as it only returns cards that are due according to the spaced repetition algorithm
     */
    @GetMapping("/study/deck/{deckId}")
    public List<Card> getCardsToStudyByDeck(@PathVariable Long deckId) {
        return reviewService.findDueCardsForDeck(deckId);
    }
    
    /**
     * Get cards that are due for review in a book
     * This returns only cards that are ready to be reviewed based on their interval
     */
    @GetMapping("/study/book/{bookId}")
    public List<Card> getCardsToStudyByBook(@PathVariable Long bookId) {
        return reviewService.findDueCardsForBook(bookId);
    }
    
    /**
     * Add a new card to a deck
     * When a card is first created, it will be immediately available for review
     * since it has no review history yet
     */
    @PostMapping("/deck/{deckId}")
    public Card createCard(@PathVariable Long deckId, @RequestBody Card card) {
        return cardService.createCardInDeck(card, deckId);
    }
    
    /**
     * Update an existing card
     * This doesn't affect the card's review schedule
     */
    @PutMapping("/{cardId}")
    public Card updateCard(@PathVariable Long cardId, @RequestBody Card card) {
        return cardService.updateCard(cardId, card);
    }
    
    /**
     * Delete a card
     */
    @DeleteMapping("/{cardId}")
    public ResponseEntity<Void> deleteCard(@PathVariable Long cardId) {
        cardService.deleteCard(cardId);
        return ResponseEntity.noContent().build();
    }
} 