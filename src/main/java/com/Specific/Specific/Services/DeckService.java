package com.Specific.Specific.Services;

import com.Specific.Specific.Except.DeckNotFoundException;
import com.Specific.Specific.Models.Entities.Deck;
import com.Specific.Specific.Models.Entities.User;
import com.Specific.Specific.Repository.DeckRepo;
import com.Specific.Specific.utils.SecurityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;

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
        return createDeck(deck, currentUser);
    }
    
    /**
     * Create a new deck for a specific user
     * 
     * @param deck The deck to create
     * @param user The user who will own the deck
     * @return The created deck
     */
    public Deck createDeck(Deck deck, User user) {
        if (deck.getUser() == null) {
            user.addDeck(deck);
        }
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
     * Get a deck by ID for a specific user
     * 
     * @param id The ID of the deck to get
     * @param user The user requesting the deck
     * @return The deck
     * @throws DeckNotFoundException If the deck doesn't exist
     */
    public Deck getDeckById(Long id, User user) {
        Deck deck = deckRepo.findById(id)
                .orElseThrow(() -> new DeckNotFoundException("Deck not found with ID: " + id));
        
        // Verify the given user has access to this deck
        // First check by user ID, then by Firebase UID
        if (deck.getUser().getId() != user.getId() && 
            !Objects.equals(deck.getUser().getFirebaseUid(), user.getFirebaseUid())) {
            throw new DeckNotFoundException("Deck not found with ID: " + id + " for this user");
        }
        
        return deck;
    }
    
    /**
     * Delete a deck by ID
     * 
     * @param id The ID of the deck to delete
     * @throws DeckNotFoundException If the deck doesn't exist
     */
    public void deleteDeck(Long id) {
        // Get the current user and verify ownership
        User currentUser = securityUtils.getCurrentUser();
        deleteDeck(id, currentUser);
    }
    
    /**
     * Delete a deck by ID for a specific user
     * 
     * @param id The ID of the deck to delete
     * @param user The user requesting deletion
     * @throws DeckNotFoundException If the deck doesn't exist
     */
    public void deleteDeck(Long id, User user) {
        // Fetch the deck first to check ownership
        Deck deck = deckRepo.findById(id)
                .orElseThrow(() -> new DeckNotFoundException("Deck not found with ID: " + id));
                
        // Verify the given user has access to this deck
        // First check by user ID, then by Firebase UID
        if (deck.getUser().getId() != user.getId() && 
            !Objects.equals(deck.getUser().getFirebaseUid(), user.getFirebaseUid())) {
            throw new DeckNotFoundException("Deck not found with ID: " + id + " for this user");
        }
        
        deckRepo.deleteById(id);
    }
    
    /**
     * Get all decks for the current user
     * 
     * @return List of decks
     */
    public List<Deck> getUserDecks() {
        User currentUser = securityUtils.getCurrentUser();
        return getUserDecks(currentUser);
    }
    
    /**
     * Get all decks for a specific user
     * 
     * @param user The user whose decks to get
     * @return List of decks
     */
    public List<Deck> getUserDecks(User user) {
        return deckRepo.findByUser(user);
    }
    
    /**
     * Get all decks for a specific user by user ID
     *
     * @return List of decks
     */
    public List<Deck> getDecksByUserId() {
        User currentUser = securityUtils.getCurrentUser();
        return deckRepo.findByUser(currentUser);
    }
}
