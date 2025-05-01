package com.Specific.Specific.Models;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonRootName;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ResponseTranslation {
    // Fields for our API response
    private String text;
    private String det_lang;
    
    // Field for DeepL API response
    private List<Translation> translations;
    
    // Default constructor for Jackson
    public ResponseTranslation() {
    }
    
    // Constructor for our API response
    public ResponseTranslation(String text, String det_lang) {
        this.text = text;
        this.det_lang = det_lang;
    }
    
    // Getters and setters
    public String getText() {
        return text;
    }
    
    public String getDet_lang() {
        return det_lang;
    }

    public List<Translation> getTranslations() {
        return translations;
    }
    
    public void setTranslations(List<Translation> translations) {
        this.translations = translations;
        
        // Auto-map from DeepL format to our format if translations are present
        if (translations != null && !translations.isEmpty()) {
            Translation translation = translations.get(0);
            this.text = translation.text;
            this.det_lang = translation.detectedSourceLanguage;
        }
    }
    
    // Nested class for DeepL's translation format
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Translation {
        private String text;
        
        @JsonProperty("detected_source_language")
        private String detectedSourceLanguage;
        
        public Translation() {
        }
        
        public String getText() {
            return text;
        }
        
        public void setText(String text) {
            this.text = text;
        }
        
        public String getDetectedSourceLanguage() {
            return detectedSourceLanguage;
        }
        
        public void setDetectedSourceLanguage(String detectedSourceLanguage) {
            this.detectedSourceLanguage = detectedSourceLanguage;
        }
    }
}
