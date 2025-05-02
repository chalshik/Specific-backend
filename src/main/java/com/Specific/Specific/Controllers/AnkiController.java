package com.Specific.Specific.Controllers;

import com.Specific.Specific.Models.Deck;
import com.Specific.Specific.Models.User;
import com.Specific.Specific.Repository.DeckRepo;
import com.Specific.Specific.Services.DeckService;
import com.Specific.Specific.Services.UserService;
import com.google.firebase.auth.FirebaseAuthException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseToken;

@RestController
@RequestMapping("/cards")
public class AnkiController {
    private UserService userService;
    private DeckService deckService;
    
    @Autowired
    public AnkiController(UserService userService, DeckService deckService){
        this.userService = userService;
        this.deckService = deckService;
    }
    
    @PostMapping("/add-deck")
    public ResponseEntity<Deck> addDeck(@RequestBody Deck deck) {
        // Get Firebase UID from authentication
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String firebaseUid = auth.getName();

        // Fetch user using Firebase UID
        User user = userService.findUserByFirebaseUid(firebaseUid);

        deck.setUser_id(user.getId());
        return ResponseEntity.ok(deckService.addDeck(deck));
    }

    @PostMapping("/delete-deck")
    public ResponseEntity<String> deleteDeck(@RequestBody Deck deck) {
        // Get Firebase UID from authentication
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String firebaseUid = auth.getName();

        // Fetch user using Firebase UID
        User user = userService.findUserByFirebaseUid(firebaseUid);
        
        // TODO: Add validation to ensure user owns the deck
        // For now, returning placeholder response
        return ResponseEntity.ok("Deck deletion not yet implemented");
    }
}
