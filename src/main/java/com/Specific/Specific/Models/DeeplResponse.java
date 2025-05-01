package com.Specific.Specific.Models;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public class DeeplResponse {
    private List<Translation> translations;

    public List<Translation> getTranslations() {
        return translations;
    }

    public void setTranslations(List<Translation> translations) {
        this.translations = translations;
    }

    public static class Translation {
        private String text;
        
        @JsonProperty("detected_source_language")
        private String detectedSourceLanguage;

        public String getText() {
            return text;
        }

        public String getDetectedSourceLanguage() {
            return detectedSourceLanguage;
        }
    }
} 