package com.Specific.Specific.Services;

import com.Specific.Specific.Models.RequestModels.RequestTranslation;
import com.Specific.Specific.Models.ResponseModels.ResponseTranslation;
import com.Specific.Specific.config.DeeplConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;

import java.util.concurrent.CompletableFuture;

/**
 * Service for handling translations using the DeepL API.
 * Translates words between different languages.
 */
@Service
public class TranslationService {
    private static final Logger logger = LoggerFactory.getLogger(TranslationService.class);
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
        logger.info("TranslationService initialized with API URL: {}", deeplConfig.getApiUrl());
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

        logger.info("Requesting translation for '{}' to {}", request.getWord(), request.getDest_lang());
        
        // Verify API key is available
        if (deeplConfig.getApiKey() == null || deeplConfig.getApiKey().isEmpty() || 
            deeplConfig.getApiKey().equals("your_api_key_for_dev")) {
            logger.error("DeepL API key is missing or invalid");
            return CompletableFuture.failedFuture(
                new RuntimeException("Translation service unavailable: API key configuration error"));
        }
        
        logger.debug("Using DeepL API key: {}", 
            deeplConfig.getApiKey().length() > 5 ? deeplConfig.getApiKey().substring(0, 5) + "..." : "invalid");
        
        // Prepare DeepL API request parameters
        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add("text", request.getWord());
        formData.add("target_lang", request.getDest_lang());
        
        // For DeepL API, also add source_lang=auto to detect language automatically
        formData.add("source_lang", "auto");

        // Add context if available (improves translation accuracy)
        if (request.getContext() != null && !request.getContext().isEmpty()) {
            formData.add("context", request.getContext());
        }

        // Log complete request for debugging
        logger.debug("DeepL API request: {}", formData);
        logger.debug("DeepL API URL: {}", deeplConfig.getApiUrl());

        // Execute the API call to DeepL
        try {
            return webClient.post()
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .header("Authorization", "DeepL-Auth-Key " + deeplConfig.getApiKey())
                    .body(BodyInserters.fromFormData(formData))
                    .retrieve()
                    .onStatus(status -> status.is4xxClientError(), response -> {
                        logger.error("DeepL API client error: {} {}", 
                            response.statusCode().value(), response.statusCode().toString());
                        return response.bodyToMono(String.class)
                            .flatMap(body -> {
                                logger.error("DeepL API error response body: {}", body);
                                return Mono.error(new RuntimeException("DeepL API client error: " + response.statusCode()));
                            });
                    })
                    .onStatus(status -> status.is5xxServerError(), response -> {
                        logger.error("DeepL API server error: {} {}", 
                            response.statusCode().value(), response.statusCode().toString());
                        return Mono.error(new RuntimeException("DeepL API server error: " + response.statusCode()));
                    })
                    .bodyToMono(ResponseTranslation.class)
                    .doOnSuccess(response -> {
                        if (response == null) {
                            logger.error("Received null response from DeepL API");
                        } else if (response.getTranslations() == null || response.getTranslations().isEmpty()) {
                            logger.error("Translation response contains no translations");
                        } else {
                            logger.info("Successfully received translation: {} (detected: {})", 
                                response.getText(), response.getDet_lang());
                        }
                    })
                    .doOnError(error -> {
                        logger.error("Error receiving translation: {}", error.getMessage(), error);
                        if (error instanceof WebClientResponseException) {
                            WebClientResponseException wcre = (WebClientResponseException) error;
                            logger.error("Response status: {}, body: {}", 
                                wcre.getStatusCode(), wcre.getResponseBodyAsString());
                        }
                    })
                    .toFuture();
        } catch (Exception e) {
            logger.error("Exception during translation request: {}", e.getMessage(), e);
            return CompletableFuture.failedFuture(
                new RuntimeException("Translation request failed: " + e.getMessage(), e));
        }
    }
}
