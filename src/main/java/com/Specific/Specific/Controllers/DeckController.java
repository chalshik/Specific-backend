package com.Specific.Specific.Controllers;

import com.Specific.Specific.Models.RequestModels.RequestDeck;
import com.Specific.Specific.Models.ResponseModels.ApiResponse;
import com.Specific.Specific.Models.Entities.Card;
import com.Specific.Specific.Models.Entities.Deck;
import com.Specific.Specific.Models.Entities.User;
import com.Specific.Specific.Services.CardService;
import com.Specific.Specific.Services.DeckService;
import com.Specific.Specific.Services.UserService;
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
    private final UserService userService;
    
    @Autowired
    public DeckController(DeckService deckService, CardService cardService, 
                          SecurityUtils securityUtils, UserService userService) {
        this.deckService = deckService;
        this.cardService = cardService;
        this.securityUtils = securityUtils;
        this.userService = userService;
    }
    
    @PostMapping("/add-deck")
    public Deck addDeck(@RequestBody RequestDeck requestDeck, 
                        @RequestParam(required = false) String firebaseUid) {
        // Get Firebase UID from request body, request parameter, or default
        String uid = requestDeck.getFirebaseUid() != null ? requestDeck.getFirebaseUid() : 
                    (firebaseUid != null ? firebaseUid : "auto-authenticated-user");
                    
        logger.info("Creating a new deck with title: {}, firebaseUid: {}", 
                  requestDeck.getTitle(), uid);
        
        // Get user directly using Firebase UID
        User user = userService.findUserByFirebaseUid(uid);
        
        Deck deck = new Deck();
        deck.setTitle(requestDeck.getTitle());
        deck.setUser(user); // Set user directly
        return deckService.createDeck(deck, user);
    }

    @DeleteMapping("/delete-deck/{deckId}")
    public ApiResponse deleteDeck(@PathVariable Long deckId,
                                 @RequestParam(required = false) String firebaseUid) {
        logger.info("Deleting deck with ID: {}, firebaseUid: {}", 
                  deckId, firebaseUid != null ? firebaseUid : "auto-authenticated-user");
        
        User user = firebaseUid != null ? 
                   userService.findUserByFirebaseUid(firebaseUid) : 
                   securityUtils.getCurrentUser();
                   
        deckService.deleteDeck(deckId, user);
        return ApiResponse.success("Deck deleted successfully");
    }
    
    @GetMapping("/user-decks")
    public List<Deck> getUserDecks(@RequestParam(required = false) String firebaseUid) {
        String uid = firebaseUid != null ? firebaseUid : "auto-authenticated-user";
        logger.info("Getting all decks for user, firebaseUid: {}", uid);
        
        User user = userService.findUserByFirebaseUid(uid);
        return deckService.getUserDecks(user);
    }
    
    @DeleteMapping("/delete-card/{cardId}")
    public ApiResponse deleteCard(@PathVariable Long cardId,
                                 @RequestParam(required = false) String firebaseUid) {
        String uid = firebaseUid != null ? firebaseUid : "auto-authenticated-user";
        logger.info("Deleting card with ID: {}, firebaseUid: {}", cardId, uid);
        
        User user = userService.findUserByFirebaseUid(uid);
        cardService.deleteCard(cardId);
        return ApiResponse.success("Card deleted successfully");
    }
}
