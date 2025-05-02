package com.Specific.Specific.Controllers;

import com.Specific.Specific.Models.ApiResponse;
import com.Specific.Specific.Models.Book;
import com.Specific.Specific.Services.BookService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/books")
public class BookController {
    
    private final BookService bookService;
    
    @Autowired
    public BookController(BookService bookService) {
        this.bookService = bookService;
    }
    
    @PostMapping
    public Book createBook(@Valid @RequestBody Book book) {
        return bookService.addBook(book);
    }
    
    @GetMapping
    public List<Book> getUserBooks() {
        return bookService.findCurrentUserBooks();
    }
    
    @GetMapping("/{bookId}")
    public Book getBookById(@PathVariable Long bookId) {
        return bookService.findBookById(bookId);
    }
    
    @GetMapping("/search")
    public List<Book> searchBooks(@RequestParam String title) {
        return bookService.searchBooksByTitle(title);
    }
    
    @PutMapping("/{bookId}")
    public Book updateBook(@PathVariable Long bookId, @Valid @RequestBody Book book) {
        return bookService.updateBook(bookId, book);
    }
    
    @DeleteMapping("/{bookId}")
    public ApiResponse deleteBook(@PathVariable Long bookId) {
        bookService.deleteBookById(bookId);
        return ApiResponse.success("Book deleted successfully");
    }
} 