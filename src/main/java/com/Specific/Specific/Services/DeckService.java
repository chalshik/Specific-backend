package com.Specific.Specific.Services;

import com.Specific.Specific.Except.DeckNotFoundException;
import com.Specific.Specific.Models.Entities.Deck;
import com.Specific.Specific.Models.Entities.User;
import com.Specific.Specific.Repository.DeckRepo;
import com.Specific.Specific.util.SecurityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class DeckService {
    private final DeckRepo deckRepo;
    private final SecurityUtils securityUtils;
    private final AuthorizationService authorizationService;
    
    @Autowired
    public DeckService(
            DeckRepo deckRepo, 
            SecurityUtils securityUtils,
            AuthorizationService authorizationService) {
        this.deckRepo = deckRepo;
        this.securityUtils = securityUtils;
        this.authorizationService = authorizationService;
    }
    
    /**
     * Create a new deck for the current user
     * 
     * @param deck The deck to create
     * @return The created deck
     */
    public Deck createDeck(Deck deck) {
        User currentUser = securityUtils.getCurrentUser();
        currentUser.addDeck(deck);
        return deckRepo.save(deck);
    }
    
    /**
     * Get a deck by ID
     * 
     * @param id The ID of the deck to get
     * @return The deck
     * @throws DeckNotFoundException If the deck doesn't exist
     */
    public Deck getDeckById(Long id) {
        Deck deck = deckRepo.findById(id)
                .orElseThrow(() -> new DeckNotFoundException("Deck not found with ID: " + id));
        
        // Verify user has access to this deck
        authorizationService.verifyDeckOwner(deck);
        
        return deck;
    }
    
    /**
     * Delete a deck by ID
     * 
     * @param id The ID of the deck to delete
     * @throws DeckNotFoundException If the deck doesn't exist
     */
    public void deleteDeck(Long id) {
        deckRepo.deleteById(id);
    }
    
    /**
     * Get all decks for the current user
     * 
     * @return List of decks
     */
    public List<Deck> getUserDecks() {
        User currentUser = securityUtils.getCurrentUser();
        return currentUser.getDecks();
    }
    
    /**
     * Get all decks for a specific user
     * 
     * @param userId The ID of the user
     * @return List of decks
     */
    public List<Deck> getDecksByUserId(Long userId) {
        return deckRepo.findByUserId(userId);
    }
}
