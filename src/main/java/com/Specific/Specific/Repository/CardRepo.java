package com.Specific.Specific.Repository;

import com.Specific.Specific.Models.Entities.Card;
import com.Specific.Specific.Models.Entities.Deck;
import com.Specific.Specific.Models.Entities.Book;
import com.Specific.Specific.Models.Entities.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.awt.font.OpenType;
import java.util.List;
import java.util.Optional;

@Repository
public interface CardRepo extends JpaRepository<Card, Long> {
    List<Card> findByUser(User user);
    List<Card> findByBook(Book book);
    List<Card> findByDeck(Deck deck);
}
