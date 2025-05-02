package com.Specific.Specific.Services;

import com.Specific.Specific.Models.Deck;
import com.Specific.Specific.Repository.DeckRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class DeckService {
    @Autowired
    private DeckRepo deckRepo;
    public Deck addDeck(Deck deck){
        return deckRepo.save(deck);
    }
}
