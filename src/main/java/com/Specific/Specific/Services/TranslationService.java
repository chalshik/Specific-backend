package com.Specific.Specific.Services;

import com.Specific.Specific.Models.DeeplResponse;
import com.Specific.Specific.Models.RequestTranslation;
import com.Specific.Specific.Models.ResponseTranslation;
import com.Specific.Specific.config.DeeplConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import org.springframework.http.HttpHeaders;
import org.springframework.web.reactive.function.client.WebClientResponseException;


@Service
public class TranslationService {
    private final WebClient webClient;
    
    public TranslationService(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder.baseUrl(DeeplConfig.getApiUrl()).build();
    }
    
    public Mono<ResponseTranslation> getTranslation(RequestTranslation request) {
        // Basic validation
        if (request.getWord() == null || request.getWord().isEmpty() || 
            request.getDest_lang() == null || request.getDest_lang().isEmpty()) {
            return Mono.error(new RuntimeException("Word and destination language cannot be empty"));
        }
        
        // Create proper form data for DeepL API
        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add("text", request.getWord());
        formData.add("target_lang", request.getDest_lang());
        
        // Add context if available
        if (request.getContext() != null && !request.getContext().isEmpty()) {
            formData.add("context", request.getContext());
        }
        
        return webClient.post()
                .uri("") // The path after the base URL
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_FORM_URLENCODED_VALUE)
                .header("Authorization", "DeepL-Auth-Key " + DeeplConfig.getAPIKEY())
                .body(BodyInserters.fromFormData(formData))
                .retrieve()
                .bodyToMono(DeeplResponse.class)
                .map(response -> {
                    if (response.getTranslations() != null && !response.getTranslations().isEmpty()) {
                        DeeplResponse.Translation translation = response.getTranslations().get(0);
                        return new ResponseTranslation(
                                translation.getText(),
                                translation.getDetectedSourceLanguage()
                        );
                    }
                    return new ResponseTranslation("", "");
                })
                .onErrorResume(WebClientResponseException.class, e -> {
                    return Mono.error(new RuntimeException("Error calling DeepL API: " + e.getMessage()));
                })
                .onErrorResume(Exception.class, e -> {
                    return Mono.error(new RuntimeException("Unexpected error: " + e.getMessage()));
                });
    }
}
