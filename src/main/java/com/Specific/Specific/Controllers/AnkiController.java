package com.Specific.Specific.Controllers;

import com.Specific.Specific.Models.Deck;
import com.Specific.Specific.Models.User;
import com.Specific.Specific.Repository.DeckRepo;
import com.Specific.Specific.Services.DeckService;
import com.Specific.Specific.Services.UserService;
import com.google.firebase.auth.FirebaseAuthException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
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
    public ResponseEntity<Deck> addDeck(@RequestBody Deck deck, @RequestHeader("Authorization") String authHeader) throws FirebaseAuthException {
        String idToken = authHeader.replace("Bearer ", "");
        FirebaseToken decodedToken = FirebaseAuth.getInstance().verifyIdToken(idToken);
        String firebaseUid = decodedToken.getUid();

        // Service throws exception if user not found
        User user = userService.findUserByEmail(firebaseUid);

        deck.setUser_id(user.getId());
        return ResponseEntity.ok(deckService.addDeck(deck));
    }

}
