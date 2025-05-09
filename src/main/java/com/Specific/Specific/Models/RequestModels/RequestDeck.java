package com.Specific.Specific.Models.RequestModels;

public class RequestDeck {
    private long id;
    private String title;
    private String firebaseUid;

    public RequestDeck() {
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getFirebaseUid() {
        return firebaseUid;
    }

    public void setFirebaseUid(String firebaseUid) {
        this.firebaseUid = firebaseUid;
    }

    public RequestDeck(long id, String title, String firebaseUid) {
        this.id = id;
        this.title = title;
        this.firebaseUid = firebaseUid;
    }

    public RequestDeck(long id, String title) {
        this.id = id;
        this.title = title;
    }
}
