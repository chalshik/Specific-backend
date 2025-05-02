package com.Specific.Specific.Models;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Entity
public class Card {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;
    
    @NotNull(message = "Deck ID is required")
    private long deckId;
    
    private long userId;
    
    @NotBlank(message = "Front content is required")
    @Size(min = 1, max = 500, message = "Front content must be between 1 and 500 characters")
    private String front;
    
    @NotBlank(message = "Back content is required")
    @Size(min = 1, max = 500, message = "Back content must be between 1 and 500 characters")
    private String back;
    
    private String context;
    
    private long bookId;

    // Constructors
    public Card() {
    }

    public Card(long id, long deckId, long userId, String front, String back, String context, long bookId) {
        this.id = id;
        this.deckId = deckId;
        this.userId = userId;
        this.front = front;
        this.back = back;
        this.context = context;
        this.bookId = bookId;
    }

    // Getters and Setters
    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public long getDeckId() {
        return deckId;
    }

    public void setDeckId(long deckId) {
        this.deckId = deckId;
    }

    public long getUserId() {
        return userId;
    }

    public void setUserId(long userId) {
        this.userId = userId;
    }

    public String getFront() {
        return front;
    }

    public void setFront(String front) {
        this.front = front;
    }

    public String getBack() {
        return back;
    }

    public void setBack(String back) {
        this.back = back;
    }

    public String getContext() {
        return context;
    }

    public void setContext(String context) {
        this.context = context;
    }

    public long getBookId() {
        return bookId;
    }

    public void setBookId(long bookId) {
        this.bookId = bookId;
    }
}
