package com.Specific.Specific.Controllers;

import com.Specific.Specific.Models.RequestModels.RequestDeck;
import com.Specific.Specific.Models.ResponseModels.ApiResponse;
import com.Specific.Specific.Models.Entities.Deck;
import com.Specific.Specific.Services.CardService;
import com.Specific.Specific.Services.DeckService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/anki")
public class DeckController {
    private final DeckService deckService;
    private final CardService cardService;
    
    @Autowired
    public DeckController(DeckService deckService, CardService cardService) {
        this.deckService = deckService;
        this.cardService = cardService;
    }
    
    @PostMapping("/add-deck")
    public Deck addDeck(@RequestBody RequestDeck requestDeck) {
        Deck deck = new Deck();
        deck.setTitle(requestDeck.getTitle());
        return deckService.createDeck(deck);
    }

    @DeleteMapping("/delete-deck/{deckId}")
    public ApiResponse deleteDeck(@PathVariable Long deckId) {
        deckService.deleteDeck(deckId);
        return ApiResponse.success("Deck deleted successfully");
    }
    
    @GetMapping("/user-decks")
    public List<Deck> getUserDecks() {
        return deckService.getUserDecks();
    }
    @DeleteMapping("/delete-card/{deckId}")
    public ApiResponse deleteCard(@PathVariable Long deckId){
        cardService.deleteCard(deckId);
        return ApiResponse.success("Deck deleted successfully");
    }


}
