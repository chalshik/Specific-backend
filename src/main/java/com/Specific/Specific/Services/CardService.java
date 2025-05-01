package com.Specific.Specific.Services;

import com.Specific.Specific.Models.Card;
import com.Specific.Specific.Repository.CardRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * Service for managing flashcard operations.
 * Handles creating, updating, and deleting cards.
 */
@Service
public class CardService {
    private final CardRepo cardRepo;
    
    @Autowired
    public CardService(CardRepo cardRepo) {
        this.cardRepo = cardRepo;
    }
    
    /**
     * Add a new flashcard to the database.
     * 
     * @param card The card entity to save
     * @return The saved card with generated ID
     */
    public Card addCard(Card card) {
       return cardRepo.save(card);
    }
    
    /**
     * Delete a flashcard from the database.
     * 
     * @param card The card to delete
     * @return The deleted card
     */
    public Card deleteCard(Card card) {
        cardRepo.delete(card);
        return card;
    }
    
    /**
     * Update an existing flashcard with new values.
     * 
     * @param newCard Card with updated values
     * @param card_id ID of the card to update
     * @return The updated card
     * @throws RuntimeException if card not found
     */
    public Card editCard(Card newCard, long card_id) {
        // Retrieve existing card or throw exception if not found
        Card oldCard = cardRepo.findById(card_id)
                .orElseThrow(() -> new RuntimeException("Card not found"));
        
        // Update card fields with new values
        oldCard.setFront(newCard.getFront());
        oldCard.setBack(newCard.getBack());
        oldCard.setContext(newCard.getContext());
        oldCard.setDeck_id(newCard.getDeck_id());
        oldCard.setBook_id(newCard.getBook_id());
        oldCard.setUser_id(newCard.getUser_id());
        
        // Save and return updated card
        return cardRepo.save(oldCard);
    }
}
