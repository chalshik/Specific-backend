package com.Specific.Specific.Controllers;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StreamUtils;
import jakarta.servlet.http.HttpServletRequest;

import java.io.IOException;
import java.nio.charset.Charset;

/**
 * Controller for serving the game website
 */
@Controller
@RequestMapping("/game")
public class WebController {

    /**
     * Serve the game homepage
     */
    @GetMapping(value = {"", "/"})
    public String index() {
        return "redirect:/game/index.html";
    }

    /**
     * Serve the game HTML file
     */
    @GetMapping("/index.html")
    @ResponseBody
    public ResponseEntity<String> getIndexHtml() throws IOException {
        Resource resource = new ClassPathResource("static/game/index.html");
        String content = StreamUtils.copyToString(resource.getInputStream(), Charset.defaultCharset());
        return ResponseEntity.ok()
                .contentType(MediaType.TEXT_HTML)
                .body(content);
    }

    /**
     * Serve CSS files
     */
    @GetMapping("/css/{filename:.+}")
    @ResponseBody
    public ResponseEntity<String> getCss(@PathVariable String filename) throws IOException {
        Resource resource = new ClassPathResource("static/game/css/" + filename);
        String content = StreamUtils.copyToString(resource.getInputStream(), Charset.defaultCharset());
        return ResponseEntity.ok()
                .contentType(MediaType.valueOf("text/css"))
                .body(content);
    }

    /**
     * Serve JavaScript files
     */
    @GetMapping("/js/{filename:.+}")
    @ResponseBody
    public ResponseEntity<String> getJs(@PathVariable String filename) throws IOException {
        Resource resource = new ClassPathResource("static/game/js/" + filename);
        String content = StreamUtils.copyToString(resource.getInputStream(), Charset.defaultCharset());
        return ResponseEntity.ok()
                .contentType(MediaType.valueOf("application/javascript"))
                .body(content);
    }
    
    /**
     * Catch-all endpoint to serve other static resources
     */
    @GetMapping("/**")
    @ResponseBody
    public ResponseEntity<String> getResource(HttpServletRequest request) throws IOException {
        String path = request.getRequestURI().substring("/game".length());
        Resource resource = new ClassPathResource("static/game" + path);
        
        if (!resource.exists()) {
            return ResponseEntity.notFound().build();
        }
        
        String content = StreamUtils.copyToString(resource.getInputStream(), Charset.defaultCharset());
        
        // Determine content type based on file extension
        String contentType = "text/plain";
        if (path.endsWith(".html")) {
            contentType = "text/html";
        } else if (path.endsWith(".css")) {
            contentType = "text/css";
        } else if (path.endsWith(".js")) {
            contentType = "application/javascript";
        } else if (path.endsWith(".json")) {
            contentType = "application/json";
        } else if (path.endsWith(".png")) {
            contentType = "image/png";
        } else if (path.endsWith(".jpg") || path.endsWith(".jpeg")) {
            contentType = "image/jpeg";
        } else if (path.endsWith(".gif")) {
            contentType = "image/gif";
        } else if (path.endsWith(".svg")) {
            contentType = "image/svg+xml";
        }
        
        return ResponseEntity.ok()
                .contentType(MediaType.valueOf(contentType))
                .body(content);
    }
}
