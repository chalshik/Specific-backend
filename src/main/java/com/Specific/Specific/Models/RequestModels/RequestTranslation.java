package com.Specific.Specific.Models.RequestModels;

public class RequestTranslation {
    private String dest_lang;
    private String word;
    private String context;

    // Default constructor required for Jackson
    public RequestTranslation() {
    }

    public RequestTranslation(String dest_lang, String word) {
        this.dest_lang = dest_lang;
        this.word = word;
    }

    public String getDest_lang() {
        return dest_lang;
    }

    public String getWord() {
        return word;
    }

    public String getContext() {
        return context;
    }
}
