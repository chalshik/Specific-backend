package com.Specific.Specific.Services;

import com.Specific.Specific.Except.DeckNotFoundException;
import com.Specific.Specific.Models.Deck;
import com.Specific.Specific.Models.User;
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
     * Add a new deck for the current user
     * 
     * @param deck The deck to add
     * @return The saved deck with generated ID
     */
    public Deck addDeck(Deck deck) {
        User currentUser = securityUtils.getCurrentUser();
        deck.setUser_id(currentUser.getId());
        return deckRepo.save(deck);
    }
    
    /**
     * Delete a deck
     * 
     * @param deck The deck to delete
     * @return The deleted deck
     */
    public Deck deleteDeck(Deck deck) {
        authorizationService.verifyDeckOwner(deck);
        deckRepo.delete(deck);
        return deck;
    }
    
    /**
     * Delete a deck by its ID
     * 
     * @param deckId The ID of the deck to delete
     * @return The deleted deck
     * @throws DeckNotFoundException if the deck is not found
     */
    public Deck deleteDeckById(Long deckId) {
        Deck deck = findDeckById(deckId);
        return deleteDeck(deck);
    }
    
    /**
     * Find a deck by its ID
     * 
     * @param deckId The ID of the deck to find
     * @return The found deck
     * @throws DeckNotFoundException if the deck is not found
     */
    public Deck findDeckById(Long deckId) {
        return deckRepo.findById(deckId)
                .orElseThrow(() -> new DeckNotFoundException("Deck with ID " + deckId + " not found"));
    }
    
    /**
     * Find all decks belonging to the current user
     * 
     * @return List of decks belonging to the current user
     */
    public List<Deck> findCurrentUserDecks() {
        User currentUser = securityUtils.getCurrentUser();
        return deckRepo.findByUser_id(currentUser.getId());
    }
    
    /**
     * Find all decks belonging to a specific user
     * 
     * @param userId The ID of the user
     * @return List of decks belonging to the user
     */
    public List<Deck> findDecksByUserId(Long userId) {
        return deckRepo.findByUser_id(userId);
    }
}
