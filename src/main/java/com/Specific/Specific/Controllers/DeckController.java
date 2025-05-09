package com.Specific.Specific.Controllers;

import com.Specific.Specific.Models.RequestModels.RequestDeck;
import com.Specific.Specific.Models.ResponseModels.ApiResponse;
import com.Specific.Specific.Models.Entities.Card;
import com.Specific.Specific.Models.Entities.Deck;
import com.Specific.Specific.Services.CardService;
import com.Specific.Specific.Services.DeckService;
import com.Specific.Specific.utils.SecurityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/anki")
public class DeckController {
    private static final Logger logger = LoggerFactory.getLogger(DeckController.class);
    private final DeckService deckService;
    private final CardService cardService;
    private final SecurityUtils securityUtils;
    
    @Autowired
    public DeckController(DeckService deckService, CardService cardService, SecurityUtils securityUtils) {
        this.deckService = deckService;
        this.cardService = cardService;
        this.securityUtils = securityUtils;
    }
    
    @PostMapping("/add-deck")
    public Deck addDeck(@RequestBody RequestDeck requestDeck, 
                        @RequestParam(required = false) String firebaseUid) {
        logger.info("Creating a new deck with title: {}, firebaseUid: {}", 
                  requestDeck.getTitle(), 
                  requestDeck.getFirebaseUid() != null ? requestDeck.getFirebaseUid() : 
                  (firebaseUid != null ? firebaseUid : "from auth context"));
        
        Deck deck = new Deck();
        deck.setTitle(requestDeck.getTitle());
        return deckService.createDeck(deck);
    }

    @DeleteMapping("/delete-deck/{deckId}")
    public ApiResponse deleteDeck(@PathVariable Long deckId,
                                 @RequestParam(required = false) String firebaseUid) {
        logger.info("Deleting deck with ID: {}, firebaseUid: {}", 
                  deckId, firebaseUid != null ? firebaseUid : "from auth context");
        
        deckService.deleteDeck(deckId);
        return ApiResponse.success("Deck deleted successfully");
    }
    
    @GetMapping("/user-decks")
    public List<Deck> getUserDecks(@RequestParam(required = false) String firebaseUid) {
        logger.info("Getting all decks for user, firebaseUid: {}", 
                  firebaseUid != null ? firebaseUid : "from auth context");
        
        return deckService.getUserDecks();
    }
    
    @DeleteMapping("/delete-card/{deckId}")
    public ApiResponse deleteCard(@PathVariable Long deckId,
                                 @RequestParam(required = false) String firebaseUid) {
        logger.info("Deleting card for deck ID: {}, firebaseUid: {}", 
                  deckId, firebaseUid != null ? firebaseUid : "from auth context");
        
        cardService.deleteCard(deckId);
        return ApiResponse.success("Card deleted successfully");
    }
}
