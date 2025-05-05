package com.Specific.Specific.Models.Entities;

import jakarta.persistence.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "users")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;
    
    private String username;
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<Book> books = new ArrayList<>();
    @Column(unique = true, nullable = false)
    private String firebaseUid;  // Renamed from "Uid" for consistency
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<Deck> decks = new ArrayList<>();
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<Review> reviews = new ArrayList<>();
    // Constructors
    public User() {
    }

    public User(String username, String firebaseUid) {
        this.username = username;
        this.firebaseUid = firebaseUid;
    }

    // Getters and Setters
    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getFirebaseUid() {
        return firebaseUid;
    }
    public void addDeck(Deck deck){
        decks.add(deck);
        deck.setUser(this);
    }

    public void setFirebaseUid(String firebaseUid) {
        this.firebaseUid = firebaseUid;
    }
    public void addBook(Book book){
        books.add(book);
        book.setUser(this);
    }
    public void addReview(Review review){
        reviews.add(review);
        review.setUser(this);
    }
}
