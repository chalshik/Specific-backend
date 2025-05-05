package com.Specific.Specific.Services;

import com.Specific.Specific.Models.RequestModels.RequestTranslation;
import com.Specific.Specific.Models.ResponseModels.ResponseTranslation;
import com.Specific.Specific.config.DeeplConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import org.springframework.http.HttpHeaders;

import java.util.concurrent.CompletableFuture;

/**
 * Service for handling translations using the DeepL API.
 * Translates words between different languages.
 */
@Service
public class TranslationService {
    private final WebClient webClient;
    private final DeeplConfig deeplConfig;
    
    /**
     * Create a new TranslationService with configured WebClient.
     * 
     * @param webClientBuilder Spring's WebClient builder
     * @param deeplConfig Configuration for DeepL API
     */
    @Autowired
    public TranslationService(WebClient.Builder webClientBuilder, DeeplConfig deeplConfig) {
        this.deeplConfig = deeplConfig;
        this.webClient = webClientBuilder.baseUrl(deeplConfig.getApiUrl()).build();
    }
    
    /**
     * Translate a word to another language using DeepL API.
     * 
     * Expected request format:
     * {
     *   "word": "hello",           // The word or text to translate (required)
     *   "dest_lang": "DE",         // Target language code (required, e.g., DE for German, FR for French)
     *   "context": "greeting"      // Optional context to improve translation accuracy
     * }
     * 
     * Returns response format:
     * {
     *   "translations": [
     *     {
     *       "detected_source_language": "EN",  // The detected source language
     *       "text": "hallo"                   // The translated text
     *     }
     *   ]
     * }
     * 
     * @param request Request containing the word, target language, and optional context
     * @return Mono<ResponseTranslation> containing the translation results from DeepL
     * @throws RuntimeException if required fields (word or dest_lang) are missing
     */
    @Async
    public CompletableFuture<ResponseTranslation> getTranslation(RequestTranslation request) {
        // Basic validation of required fields
        if (request.getWord() == null || request.getWord().isEmpty() ||
                request.getDest_lang() == null || request.getDest_lang().isEmpty()) {
            return CompletableFuture.failedFuture(new RuntimeException("Word and destination language cannot be empty"));
        }

        // Prepare DeepL API request parameters
        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add("text", request.getWord());
        formData.add("target_lang", request.getDest_lang());

        // Add context if available (improves translation accuracy)
        if (request.getContext() != null && !request.getContext().isEmpty()) {
            formData.add("context", request.getContext());
        }

        // Execute the API call to DeepL
        Mono<ResponseTranslation> responseTranslationMono = webClient.post()
                .uri("") // The path after the base URL
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_FORM_URLENCODED_VALUE)
                .header("Authorization", "DeepL-Auth-Key " + deeplConfig.getApiKey())
                .body(BodyInserters.fromFormData(formData))
                .retrieve()
                .bodyToMono(ResponseTranslation.class);

        // Convert the Mono to CompletableFuture and return
        return responseTranslationMono.toFuture();
    }
}
