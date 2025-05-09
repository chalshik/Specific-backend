package com.Specific.Specific.Models.Entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "card")
public class Card {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "deck_id", nullable = false)
    private Deck deck;
    
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    
    @NotBlank(message = "Front content is required")
    @Size(min = 1, max = 500, message = "Front content must be between 1 and 500 characters")
    private String front;
    @JsonIgnore
    @OneToMany(mappedBy = "card", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<Review> reviews = new ArrayList<>();
    @NotBlank(message = "Back content is required")
    @Size(min = 1, max = 500, message = "Back content must be between 1 and 500 characters")
    private String back;
    
    private String context;
    
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "book_id")
    private Book book;

    // Used only for request binding, not persisted
    @Transient
    private String firebaseUid;

    // Constructors
    public Card() {
    }

    public Card(long id, String front, String back, String context) {
        this.id = id;
        this.front = front;
        this.back = back;
        this.context = context;
    }
    
    /**
     * Preferred constructor without ID (which is auto-generated)
     */
    public Card(String front, String back, String context) {
        this.front = front;
        this.back = back;
        this.context = context;
    }

    // Getters and Setters
    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public Deck getDeck() {
        return deck;
    }

    public void setDeck(Deck deck) {
        this.deck = deck;
    }
    
    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
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

    public Book getBook() {
        return book;
    }

    public void setBook(Book book) {
        this.book = book;
    }

    public Long getBookId() {
        return book != null ? book.getId() : null;
    }

    public void setBookId(long bookId) {
        // This method only creates a placeholder that won't be properly loaded
        // For proper association, use CardService.setCardBook() instead
        if (bookId > 0) {
            Book placeholder = new Book();
            placeholder.setId(bookId);
            this.book = placeholder;
        } else {
            this.book = null;
        }
    }

    public void addReview(Review review){
        reviews.add(review);
        review.setCard(this);
    }

    public String getFirebaseUid() {
        return firebaseUid;
    }
    
    public void setFirebaseUid(String firebaseUid) {
        this.firebaseUid = firebaseUid;
    }
}
