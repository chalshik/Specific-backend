package com.Specific.Specific.Services;

import com.Specific.Specific.Except.CardNotFoundException;
import com.Specific.Specific.Models.Entities.Card;
import com.Specific.Specific.Models.Entities.Deck;
import com.Specific.Specific.Models.Entities.User;
import com.Specific.Specific.Models.Entities.Book;
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
        // Set the current user
        User currentUser = securityUtils.getCurrentUser();
        card.setUser(currentUser);
        
        // Verify that the deck and book exist and belong to the user
        // This will throw exceptions if not found or not authorized
        if (card.getBook() != null) {
            Book book = bookService.getBookById(card.getBook().getId());
            card.setBook(book);
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


        
        return card;
    }
    
    /**
     * Get all cards for the current user
     * 
     * @return List of cards
     */
    public List<Card> getUserCards() {
        User currentUser = securityUtils.getCurrentUser();
        return cardRepo.findByUser(currentUser);
    }
    
    /**
     * Get all cards in a deck
     * 
     * @param deckId The ID of the deck
     * @return List of cards in the deck
     */
    public List<Card> getCardsByDeck(Long deckId) {
        // First verify the user has access to this deck
        Deck deck = deckService.getDeckById(deckId);
        
        // Now get all cards for this deck
        return cardRepo.findByDeck(deck);
    }
    
    /**
     * Get all cards in a book
     * 
     * @param bookId The ID of the book
     * @return List of cards in the book
     */
    public List<Card> getCardsByBook(Long bookId) {
        // First verify the user has access to this book
        Book book = bookService.getBookById(bookId);
        
        // Now get all cards for this book
        return cardRepo.findByBook(book);
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

        // Ensure the user field remains the same
        cardDetails.setUser(existingCard.getUser());
        
        // If deck ID has changed, verify the user has access to the new deck
        if (!cardDetails.getDeck().equals(existingCard.getDeck())) {
            Deck deck = deckService.getDeckById(cardDetails.getDeck().getId());
            cardDetails.setDeck(deck);
        }
        
        // If book has changed, verify the user has access to the new book
        if ((existingCard.getBook() == null && cardDetails.getBook() != null) || 
            (existingCard.getBook() != null && cardDetails.getBook() != null && 
             existingCard.getBook().getId() != cardDetails.getBook().getId())) {
            Book book = bookService.getBookById(cardDetails.getBook().getId());
            cardDetails.setBook(book);
        }
        
        // Update fields
        existingCard.setFront(cardDetails.getFront());
        existingCard.setBack(cardDetails.getBack());
        existingCard.setContext(cardDetails.getContext());
        existingCard.setDeck(cardDetails.getDeck());
        existingCard.setBook(cardDetails.getBook());
        
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
        
        cardRepo.delete(card);
    }
    
    /**
     * Set the book for a card by book ID
     * 
     * @param card The card to update
     * @param bookId The ID of the book to associate with the card
     * @return The updated card
     */
    public Card setCardBook(Card card, Long bookId) {
        if (bookId != null && bookId > 0) {
            Book book = bookService.getBookById(bookId);
            card.setBook(book);
        } else {
            card.setBook(null);
        }
        return card;
    }
    
    /**
     * Create a new card and associate it with a deck
     * 
     * @param card The card to create
     * @param deckId The ID of the deck to associate with the card
     * @return The created card
     */
    public Card createCardInDeck(Card card, Long deckId) {
        // Get the deck and verify access
        Deck deck = deckService.getDeckById(deckId);
        
        // Set the current user
        User currentUser = securityUtils.getCurrentUser();
        card.setUser(currentUser);
        
        // Associate the card with the deck using the helper method
        deck.addCard(card);
        
        // Set the book if needed
        if (card.getBook() != null && card.getBook().getId() > 0) {
            setCardBook(card, card.getBook().getId());
        }
        
        // Save and return
        return cardRepo.save(card);
    }
}
