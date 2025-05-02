package com.Specific.Specific.Services;

import com.Specific.Specific.Except.CardNotFoundException;
import com.Specific.Specific.Models.Card;
import com.Specific.Specific.Models.User;
import com.Specific.Specific.util.SecurityUtils;
import com.Specific.Specific.Repository.CardRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Service for managing flashcard operations.
 * Handles creating, updating, and deleting cards.
 */
@Service
public class CardService {
    private final CardRepo cardRepo;
    private final DeckService deckService;
    private final BookService bookService;
    private final AuthorizationService authorizationService;
    private final SecurityUtils securityUtils;
    
    @Autowired
    public CardService(CardRepo cardRepo, DeckService deckService, BookService bookService, 
            AuthorizationService authorizationService, SecurityUtils securityUtils) {
        this.cardRepo = cardRepo;
        this.deckService = deckService;
        this.bookService = bookService;
        this.authorizationService = authorizationService;
        this.securityUtils = securityUtils;
    }
    
    /**
     * Create a new card
     * 
     * @param card The card to create
     * @return The created card
     */
    public Card createCard(Card card) {
        // Set the user ID to the current user
        card.setUserId(securityUtils.getCurrentUser().getId());
        
        // Verify that the deck and book exist and belong to the user
        // This will throw exceptions if not found or not authorized
        deckService.getDeckById(card.getDeckId());
        if (card.getBookId() > 0) {
            bookService.getBookById(card.getBookId());
        }
        
        return cardRepo.save(card);
    }
    
    /**
     * Get a card by ID
     * 
     * @param id The ID of the card to get
     * @return The card
     * @throws CardNotFoundException If the card doesn't exist
     */
    public Card getCardById(Long id) throws CardNotFoundException {
        Card card = cardRepo.findById(id)
            .orElseThrow(() -> new CardNotFoundException("Card not found with ID: " + id));
        
        // Verify user has access to this card
        authorizationService.verifyResourceOwner(card.getUserId());
        
        return card;
    }
    
    /**
     * Get all cards for the current user
     * 
     * @return List of cards
     */
    public List<Card> getUserCards() {
        User currentUser = securityUtils.getCurrentUser();
        return cardRepo.findByUserId(currentUser.getId());
    }
    
    /**
     * Get all cards in a deck
     * 
     * @param deckId The ID of the deck
     * @return List of cards in the deck
     */
    public List<Card> getCardsByDeck(Long deckId) {
        // First verify the user has access to this deck
        deckService.getDeckById(deckId);
        
        // Now get all cards for this deck
        return cardRepo.findByDeckId(deckId);
    }
    
    /**
     * Get all cards in a book
     * 
     * @param bookId The ID of the book
     * @return List of cards in the book
     */
    public List<Card> getCardsByBook(Long bookId) {
        // First verify the user has access to this book
        bookService.getBookById(bookId);
        
        // Now get all cards for this book
        return cardRepo.findByBookId(bookId);
    }
    
    /**
     * Update a card
     * 
     * @param id The ID of the card to update
     * @param cardDetails The updated card details
     * @return The updated card
     * @throws CardNotFoundException If the card doesn't exist
     */
    public Card updateCard(Long id, Card cardDetails) throws CardNotFoundException {
        Card existingCard = cardRepo.findById(id)
            .orElseThrow(() -> new CardNotFoundException("Card not found with ID: " + id));
        
        // Verify user has access to this card
        authorizationService.verifyResourceOwner(existingCard.getUserId());
        
        // If deck ID has changed, verify the user has access to the new deck
        if (cardDetails.getDeckId() != existingCard.getDeckId()) {
            deckService.getDeckById(cardDetails.getDeckId());
        }
        
        // If book ID has changed, verify the user has access to the new book
        if (cardDetails.getBookId() != existingCard.getBookId() && cardDetails.getBookId() > 0) {
            bookService.getBookById(cardDetails.getBookId());
        }
        
        // Update fields
        existingCard.setFront(cardDetails.getFront());
        existingCard.setBack(cardDetails.getBack());
        existingCard.setContext(cardDetails.getContext());
        existingCard.setDeckId(cardDetails.getDeckId());
        existingCard.setBookId(cardDetails.getBookId());
        
        return cardRepo.save(existingCard);
    }
    
    /**
     * Delete a card by ID
     * 
     * @param id The ID of the card to delete
     * @throws CardNotFoundException If the card doesn't exist
     */
    public void deleteCard(Long id) throws CardNotFoundException {
        Card card = cardRepo.findById(id)
            .orElseThrow(() -> new CardNotFoundException("Card not found with ID: " + id));
        
        // Verify user has access to this card
        authorizationService.verifyResourceOwner(card.getUserId());
        
        cardRepo.delete(card);
    }
}
