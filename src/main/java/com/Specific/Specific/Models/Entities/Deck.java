package com.Specific.Specific.Models.Entities;

import jakarta.persistence.*;

@Entity
@Table(name = "deck")
public class Deck {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;
    private long userId;
    private String title;

    // Constructors
    public Deck() {
    }

    public Deck(long id, long userId, String title) {
        this.id = id;
        this.userId = userId;
        this.title = title;
    }

    // Getters and Setters
    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public long getUserId() {
        return userId;
    }

    public void setUserId(long userId) {
        this.userId = userId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }
}
