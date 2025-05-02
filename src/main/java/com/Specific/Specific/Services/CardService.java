package com.Specific.Specific.Services;

import com.Specific.Specific.Except.CardNotFoundException;
import com.Specific.Specific.Models.Card;
import com.Specific.Specific.Repository.CardRepo;
import com.Specific.Specific.util.SecurityUtils;
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
    private final AuthorizationService authorizationService;
    private final SecurityUtils securityUtils;
    
    @Autowired
    public CardService(
            CardRepo cardRepo,
            AuthorizationService authorizationService,
            SecurityUtils securityUtils) {
        this.cardRepo = cardRepo;
        this.authorizationService = authorizationService;
        this.securityUtils = securityUtils;
    }
    
    /**
     * Add a new flashcard to the database.
     * 
     * @param card The card entity to save
     * @return The saved card with generated ID
     */
    public Card addCard(Card card) {
        // Set current user as owner
        card.setUser_id(securityUtils.getCurrentUser().getId());
        return cardRepo.save(card);
    }
    
    /**
     * Delete a flashcard from the database.
     * 
     * @param card The card to delete
     * @return The deleted card
     */
    public Card deleteCard(Card card) {
        authorizationService.verifyResourceOwner(card.getUser_id());
        cardRepo.delete(card);
        return card;
    }
    
    /**
     * Delete a card by its ID
     * 
     * @param cardId The ID of the card to delete
     */
    public void deleteCardById(Long cardId) {
        Card card = findCardById(cardId);
        deleteCard(card);
    }
    
    /**
     * Find a card by its ID
     * 
     * @param cardId The ID of the card
     * @return The found card
     * @throws CardNotFoundException if card not found
     */
    public Card findCardById(Long cardId) {
        return cardRepo.findById(cardId)
                .orElseThrow(() -> new CardNotFoundException("Card with ID " + cardId + " not found"));
    }
    
    /**
     * Find all cards in a deck
     * 
     * @param deckId The ID of the deck
     * @return List of cards in the deck
     */
    public List<Card> findCardsByDeckId(Long deckId) {
        return cardRepo.findByDeck_id(deckId);
    }
    
    /**
     * Update an existing card with new values.
     * 
     * @param cardId ID of the card to update
     * @param newCard Card with updated values
     * @return The updated card
     */
    public Card updateCard(Long cardId, Card newCard) {
        Card existingCard = findCardById(cardId);
        
        // Verify ownership
        authorizationService.verifyResourceOwner(existingCard.getUser_id());
        
        // Update card fields
        existingCard.setFront(newCard.getFront());
        existingCard.setBack(newCard.getBack());
        existingCard.setContext(newCard.getContext());
        
        // Save and return updated card
        return cardRepo.save(existingCard);
    }
}
