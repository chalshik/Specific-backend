package com.Specific.Specific.Controllers;

import com.Specific.Specific.Models.RequestTranslation;
import com.Specific.Specific.Models.ResponseTranslation;
import com.Specific.Specific.Services.TranslationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/translation")
public class TranslationController {
    @Autowired
    private TranslationService translationService;
    
    @PostMapping
    public Mono<ResponseTranslation> getTranslation(@RequestBody RequestTranslation requestTranslation) {
        return translationService.getTranslation(requestTranslation);
    }
    
    @GetMapping
    public String testEndpoint() {
        return "Translation API is working!";
    }
}
