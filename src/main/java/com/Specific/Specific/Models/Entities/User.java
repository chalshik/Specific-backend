package com.Specific.Specific.Models.Entities;

import jakarta.persistence.*;

@Entity
@Table(name = "users")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;
    
    private String username;
    
    @Column(unique = true, nullable = false)
    private String firebaseUid;  // Renamed from "Uid" for consistency

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

    public void setFirebaseUid(String firebaseUid) {
        this.firebaseUid = firebaseUid;
    }
}
