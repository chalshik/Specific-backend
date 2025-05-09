package com.Specific.Specific.Controllers;

import com.Specific.Specific.Models.Entities.Card;
import com.Specific.Specific.Models.Entities.User;
import com.Specific.Specific.Models.Entities.Deck;
import com.Specific.Specific.Models.ResponseModels.ApiResponse;
import com.Specific.Specific.Services.CardService;
import com.Specific.Specific.Services.ReviewService;
import com.Specific.Specific.Services.DeckService;
import com.Specific.Specific.Services.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/cards")
public class CardController {
    
    private static final Logger logger = LoggerFactory.getLogger(CardController.class);
    private final CardService cardService;
    private final ReviewService reviewService;
    private final DeckService deckService;
    private final UserService userService;
    
    @Autowired
    public CardController(CardService cardService, ReviewService reviewService, 
                          DeckService deckService, UserService userService) {
        this.cardService = cardService;
        this.reviewService = reviewService;
        this.deckService = deckService;
        this.userService = userService;
    }
    
    /**
     * Get cards that are due for review in a deck
     * This is the primary endpoint users should use to study their cards,
     * as it only returns cards that are due according to the spaced repetition algorithm
     */
    @GetMapping("/study/deck/{deckId}")
    public List<Card> getCardsToStudyByDeck(
            @PathVariable Long deckId,
            @RequestParam(required = false) String firebaseUid,
            @RequestBody(required = false) Object requestBody) {
        // Extract Firebase UID from various sources
        String uid = extractFirebaseUid(firebaseUid, requestBody);
        logger.info("Getting cards to study for deck ID: {}, firebaseUid: {}", deckId, uid);
        
        // Get user directly
        User user = userService.findUserByFirebaseUid(uid);
        
        // Call service with user object
        return reviewService.findDueCardsForDeck(deckId, user);
    }
    
    /**
     * Get cards that are due for review in a book
     * This returns only cards that are ready to be reviewed based on their interval
     */
    @GetMapping("/study/book/{bookId}")
    public List<Card> getCardsToStudyByBook(
            @PathVariable Long bookId,
            @RequestParam(required = false) String firebaseUid,
            @RequestBody(required = false) Object requestBody) {
        // Extract Firebase UID from various sources
        String uid = extractFirebaseUid(firebaseUid, requestBody);
        logger.info("Getting cards to study for book ID: {}, firebaseUid: {}", bookId, uid);
        
        // Get user directly
        User user = userService.findUserByFirebaseUid(uid);
        
        // Call service with user object
        return reviewService.findDueCardsForBook(bookId, user);
    }
    
    /**
     * Get all cards in a deck
     */
    @GetMapping("/deck/{deckId}")
    public List<Card> getCardsByDeck(
            @PathVariable Long deckId,
            @RequestParam(required = false) String firebaseUid,
            @RequestBody(required = false) Object requestBody) {
        // Extract Firebase UID from various sources
        String uid = extractFirebaseUid(firebaseUid, requestBody);
        logger.info("Getting all cards for deck ID: {}, firebaseUid: {}", deckId, uid);
        
        // Get user directly
        User user = userService.findUserByFirebaseUid(uid);
        
        // Call service with user object
        return cardService.getCardsByDeck(deckId, user);
    }
    
    /**
     * Get a specific card by ID
     */
    @GetMapping("/{cardId}")
    public Card getCardById(
            @PathVariable Long cardId,
            @RequestParam(required = false) String firebaseUid,
            @RequestBody(required = false) Object requestBody) {
        // Extract Firebase UID from various sources
        String uid = extractFirebaseUid(firebaseUid, requestBody);
        logger.info("Getting card ID: {}, firebaseUid: {}", cardId, uid);
        
        // Get user directly
        User user = userService.findUserByFirebaseUid(uid);
        
        // Call service with user object
        return cardService.getCardById(cardId, user);
    }
    
    /**
     * Add a new card to a deck
     * When a card is first created, it will be immediately available for review
     * since it has no review history yet
     */
    @PostMapping
    public Card createCard(
            @RequestBody Card card,
            @RequestParam(required = false) String firebaseUid) {
        // Extract Firebase UID from card or request parameter
        String uid = card.getFirebaseUid() != null ? card.getFirebaseUid() : 
                    (firebaseUid != null ? firebaseUid : "auto-authenticated-user");
        
        logger.info("Creating card with Firebase UID: {}", uid);
        
        // Get user directly
        User user = userService.findUserByFirebaseUid(uid);
        
        // Create card with user object
        return cardService.createCard(card, user);
    }
    
    /**
     * Add a new card to a deck
     * When a card is first created, it will be immediately available for review
     * since it has no review history yet
     */
    @PostMapping("/deck/{deckId}")
    public Card createCardInDeck(
            @PathVariable Long deckId, 
            @RequestBody Card card,
            @RequestParam(required = false) String firebaseUid) {
        try {
            // Extract Firebase UID from card or request parameter
            String uid = card.getFirebaseUid() != null ? card.getFirebaseUid() : 
                        (firebaseUid != null ? firebaseUid : "auto-authenticated-user");
            
            logger.info("Creating card in deck ID: {}, firebaseUid: {}", deckId, uid);
            
            // Get user directly
            User user = userService.findUserByFirebaseUid(uid);
            if (user == null) {
                logger.error("User not found with Firebase UID: {}", uid);
                throw new RuntimeException("User with Firebase UID not found: " + uid);
            }
            
            logger.info("Found user: id={}, username={}, firebaseUid={}", 
                      user.getId(), user.getUsername(), user.getFirebaseUid());
            
            // Set the user ID of the request to match existing user in DB if needed
            card.setUser(user);
            
            // Pre-set deck association in card
            Deck deck = new Deck();
            deck.setId(deckId);
            card.setDeck(deck);
            
            logger.info("Card prepared for creation: front={}, back={}, user={}, deck={}", 
                      card.getFront(), card.getBack(), user.getId(), deckId);
            
            // Create card with user object
            Card createdCard = cardService.createCardInDeck(card, deckId, user);
            logger.info("Card created successfully with ID: {}", createdCard.getId());
            return createdCard;
        } catch (Exception e) {
            logger.error("Error creating card in deck: {}", e.getMessage(), e);
            throw e;
        }
    }
    
    /**
     * Update an existing card
     * This doesn't affect the card's review schedule
     */
    @PutMapping("/{cardId}")
    public Card updateCard(
            @PathVariable Long cardId, 
            @RequestBody Card card,
            @RequestParam(required = false) String firebaseUid) {
        // Extract Firebase UID from card or request parameter
        String uid = card.getFirebaseUid() != null ? card.getFirebaseUid() : 
                    (firebaseUid != null ? firebaseUid : "auto-authenticated-user");
        
        logger.info("Updating card ID: {}, firebaseUid: {}", cardId, uid);
        
        // Get user directly
        User user = userService.findUserByFirebaseUid(uid);
        
        // Set card ID from path variable
        card.setId(cardId);
        
        // Update card with user object
        return cardService.updateCard(cardId, card, user);
    }
    
    /**
     * Delete a card
     */
    @DeleteMapping("/{cardId}")
    public ResponseEntity<?> deleteCard(
            @PathVariable Long cardId,
            @RequestParam(required = false) String firebaseUid,
            @RequestBody(required = false) Object requestBody) {
        // Extract Firebase UID from various sources
        String uid = extractFirebaseUid(firebaseUid, requestBody);
        logger.info("Deleting card ID: {}, firebaseUid: {}", cardId, uid);
        
        // Get user directly
        User user = userService.findUserByFirebaseUid(uid);
        
        // Delete card with user object
        cardService.deleteCard(cardId, user);
        return ResponseEntity.ok(ApiResponse.success("Card deleted successfully"));
    }
    
    /**
     * Helper method to extract Firebase UID from various sources
     */
    private String extractFirebaseUid(String paramFirebaseUid, Object requestBody) {
        // First try from request parameter
        if (paramFirebaseUid != null && !paramFirebaseUid.isEmpty()) {
            return paramFirebaseUid;
        }
        
        // Then try from request body if it's a map
        if (requestBody instanceof java.util.Map) {
            Object bodyFirebaseUid = ((java.util.Map<?, ?>) requestBody).get("firebaseUid");
            if (bodyFirebaseUid != null) {
                return bodyFirebaseUid.toString();
            }
        }
        
        // Default value
        return "auto-authenticated-user";
    }
} 