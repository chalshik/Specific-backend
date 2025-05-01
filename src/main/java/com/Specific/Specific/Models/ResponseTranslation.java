package com.Specific.Specific.Models;

public class ResponseTranslation {
    private String text;
    private String det_lang;

    public ResponseTranslation() {
        // Default constructor for Jackson
    }

    public ResponseTranslation(String text, String det_lang) {
        this.text = text;
        this.det_lang = det_lang;
    }

    public String getText() {
        return text;
    }

    public String getDet_lang() {
        return det_lang;
    }
}
