package com.Specific.Specific.Controllers;

import com.Specific.Specific.Models.ResponseModels.ApiResponse;
import com.Specific.Specific.Models.Entities.Book;
import com.Specific.Specific.Services.BookService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/books")
public class BookController {
    
    private static final Logger logger = LoggerFactory.getLogger(BookController.class);
    private final BookService bookService;
    
    @Autowired
    public BookController(BookService bookService) {
        this.bookService = bookService;
    }
    
    @PostMapping
    public Book createBook(
            @Valid @RequestBody Book book,
            @RequestParam(required = false) String firebaseUid) {
        logger.info("Creating new book: {}, firebaseUid: {}", 
                  book.getTitle(), firebaseUid != null ? firebaseUid : "from auth context");
        
        return bookService.createBook(book);
    }
    
    @GetMapping
    public List<Book> getUserBooks(
            @RequestParam(required = false) String firebaseUid) {
        logger.info("Getting user books, firebaseUid: {}", 
                  firebaseUid != null ? firebaseUid : "from auth context");
        
        return bookService.getUserBooks();
    }
    
    @GetMapping("/{bookId}")
    public Book getBookById(
            @PathVariable Long bookId,
            @RequestParam(required = false) String firebaseUid) {
        logger.info("Getting book by ID: {}, firebaseUid: {}", 
                  bookId, firebaseUid != null ? firebaseUid : "from auth context");
        
        return bookService.getBookById(bookId);
    }
    
    @GetMapping("/search")
    public List<Book> searchBooks(
            @RequestParam String title,
            @RequestParam(required = false) String firebaseUid) {
        logger.info("Searching for books with title: {}, firebaseUid: {}", 
                  title, firebaseUid != null ? firebaseUid : "from auth context");
        
        return bookService.searchBooksByTitle(title);
    }
    
    @PutMapping("/{bookId}")
    public Book updateBook(
            @PathVariable Long bookId, 
            @Valid @RequestBody Book book,
            @RequestParam(required = false) String firebaseUid) {
        logger.info("Updating book ID: {}, firebaseUid: {}", 
                  bookId, firebaseUid != null ? firebaseUid : "from auth context");
        
        return bookService.updateBook(bookId, book);
    }
    
    @DeleteMapping("/{bookId}")
    public ApiResponse deleteBook(
            @PathVariable Long bookId,
            @RequestParam(required = false) String firebaseUid) {
        logger.info("Deleting book ID: {}, firebaseUid: {}", 
                  bookId, firebaseUid != null ? firebaseUid : "from auth context");
        
        bookService.deleteBook(bookId);
        return ApiResponse.success("Book deleted successfully");
    }
} 