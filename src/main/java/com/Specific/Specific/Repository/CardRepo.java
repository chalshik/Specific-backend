package com.Specific.Specific.Repository;

import com.Specific.Specific.Models.Card;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CardRepo extends JpaRepository<Card, Long> {
    List<Card> findByDeck_id(Long deckId);
    List<Card> findByUser_id(Long userId);
}
