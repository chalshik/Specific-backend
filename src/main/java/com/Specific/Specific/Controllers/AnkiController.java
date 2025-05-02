package com.Specific.Specific.Controllers;

import com.Specific.Specific.Models.ApiResponse;
import com.Specific.Specific.Models.Deck;
import com.Specific.Specific.Services.DeckService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/cards")
public class AnkiController {
    private final DeckService deckService;
    
    @Autowired
    public AnkiController(DeckService deckService) {
        this.deckService = deckService;
    }
    
    @PostMapping("/add-deck")
    public Deck addDeck(@RequestBody Deck deck) {
        return deckService.addDeck(deck);
    }

    @DeleteMapping("/delete-deck/{deckId}")
    public ApiResponse deleteDeck(@PathVariable Long deckId) {
        deckService.deleteDeckById(deckId);
        return ApiResponse.success("Deck deleted successfully");
    }
    
    @GetMapping("/user-decks")
    public List<Deck> getUserDecks() {
        return deckService.findCurrentUserDecks();
    }
}
