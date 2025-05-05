package com.Specific.Specific.Models.Entities;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;

@Entity
@Table(name = "review")
public class Review {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    @NotNull
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "card_id", nullable = false)
    private Card card;

    @NotNull
    @Column(name = "review_date", nullable = false)
    private LocalDateTime reviewDate;

    @NotNull
    @Column(name = "ease_factor", nullable = false)
    private Double easeFactor;

    @NotNull
    @Column(name = "interval", nullable = false)
    private Integer interval;

    @NotNull
    @Column(name = "repetitions", nullable = false)
    private Integer repetitions;

    public enum Rating { AGAIN, HARD, GOOD, EASY }

    @Enumerated(EnumType.STRING)
    @Column(name = "last_result", nullable = true)
    private Rating lastResult;
    
    // Constructors
    public Review() {
    }
    
    public Review(Long id, LocalDateTime reviewDate,
                 Double easeFactor, Integer interval, Integer repetitions, Rating lastResult) {
        this.id = id;
        this.reviewDate = reviewDate;
        this.easeFactor = easeFactor;
        this.interval = interval;
        this.repetitions = repetitions;
        this.lastResult = lastResult;
    }
    
    // Getters and Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public Card getCard() {
        return card;
    }

    public void setCard(Card card) {
        this.card = card;
    }

    public LocalDateTime getReviewDate() {
        return reviewDate;
    }
    
    public void setReviewDate(LocalDateTime reviewDate) {
        this.reviewDate = reviewDate;
    }
    
    public Double getEaseFactor() {
        return easeFactor;
    }
    
    public void setEaseFactor(Double easeFactor) {
        this.easeFactor = easeFactor;
    }
    
    public Integer getInterval() {
        return interval;
    }
    
    public void setInterval(Integer interval) {
        this.interval = interval;
    }
    
    public Integer getRepetitions() {
        return repetitions;
    }
    
    public void setRepetitions(Integer repetitions) {
        this.repetitions = repetitions;
    }
    
    public Rating getLastResult() {
        return lastResult;
    }
    
    public void setLastResult(Rating lastResult) {
        this.lastResult = lastResult;
    }
}