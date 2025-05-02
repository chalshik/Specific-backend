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
    private long deck_id;
    
    private long user_id;
    
    @NotBlank(message = "Front content is required")
    @Size(min = 1, max = 500, message = "Front content must be between 1 and 500 characters")
    private String front;
    
    @NotBlank(message = "Back content is required")
    @Size(min = 1, max = 500, message = "Back content must be between 1 and 500 characters")
    private String back;
    
    private String context;
    
    private long book_id;

    // Constructors
    public Card() {
    }

    public Card(long id, long deck_id, long user_id, String front, String back, String context, long book_id) {
        this.id = id;
        this.deck_id = deck_id;
        this.user_id = user_id;
        this.front = front;
        this.back = back;
        this.context = context;
        this.book_id = book_id;
    }

    // Getters and Setters
    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public long getDeck_id() {
        return deck_id;
    }

    public void setDeck_id(long deck_id) {
        this.deck_id = deck_id;
    }

    public long getUser_id() {
        return user_id;
    }

    public void setUser_id(long user_id) {
        this.user_id = user_id;
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

    public long getBook_id() {
        return book_id;
    }

    public void setBook_id(long book_id) {
        this.book_id = book_id;
    }
}
