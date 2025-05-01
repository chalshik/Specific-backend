package com.Specific.Specific.Repository;

import com.Specific.Specific.Models.Deck;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DeckRepo extends JpaRepository<Deck,Long> {
}
