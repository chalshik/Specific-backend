package com.Specific.Specific.Services;

import com.Specific.Specific.Except.BookNotFoundException;
import com.Specific.Specific.Models.Entities.Book;
import com.Specific.Specific.Models.Entities.User;
import com.Specific.Specific.Repository.BookRepo;
import com.Specific.Specific.util.SecurityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class BookService {
    private final BookRepo bookRepo;
    private final SecurityUtils securityUtils;
    private final AuthorizationService authorizationService;
    
    @Autowired
    public BookService(
            BookRepo bookRepo,
            SecurityUtils securityUtils,
            AuthorizationService authorizationService) {
        this.bookRepo = bookRepo;
        this.securityUtils = securityUtils;
        this.authorizationService = authorizationService;
    }
    
    /**
     * Create a new book
     *
     * @param book The book to create
     * @return The created book
     */
    public Book createBook(Book book) {
        User currentUser = securityUtils.getCurrentUser();
        
        // Set user ID
        book.setUser(currentUser);
        
        return bookRepo.save(book);
    }
    
    /**
     * Get a book by ID
     *
     * @param id The ID of the book to get
     * @return The book
     * @throws BookNotFoundException If the book doesn't exist
     */
    public Book getBookById(Long id) throws BookNotFoundException {
        Book book = bookRepo.findById(id)
            .orElseThrow(() -> new BookNotFoundException("Book not found with ID: " + id));
        
        // Verify user has access to this book
        authorizationService.verifyResourceOwner(book.getUser().getId());
        
        return book;
    }
    
    /**
     * Delete a book by ID
     *
     * @param id The ID of the book to delete
     * @throws BookNotFoundException If the book doesn't exist
     */
    public void deleteBook(Long id) throws BookNotFoundException {
        Book book = bookRepo.findById(id)
            .orElseThrow(() -> new BookNotFoundException("Book not found with ID: " + id));
        
        // Verify user has access to this book
        authorizationService.verifyResourceOwner(book.getUser().getId());
        
        bookRepo.delete(book);
    }
    
    /**
     * Get all books for the current user
     *
     * @return List of books
     */
    public List<Book> getUserBooks() {
        User currentUser = securityUtils.getCurrentUser();
        return bookRepo.findByUserId(currentUser.getId());
    }
    
    /**
     * Search books by title (case insensitive)
     *
     * @param title The title to search for
     * @return List of matching books
     */
    public List<Book> searchBooksByTitle(String title) {
        // This assumes the repository has a method for this
        return bookRepo.findByTitleContainingIgnoreCase(title);
    }
    
    /**
     * Update a book
     *
     * @param id The ID of the book to update
     * @param bookDetails The updated book details
     * @return The updated book
     * @throws BookNotFoundException If the book doesn't exist
     */
    public Book updateBook(Long id, Book bookDetails) throws BookNotFoundException {
        Book existingBook = bookRepo.findById(id)
            .orElseThrow(() -> new BookNotFoundException("Book not found with ID: " + id));

        authorizationService.verifyResourceOwner(existingBook.getUser().getId());
        // Update fields
        existingBook.setTitle(bookDetails.getTitle());
        
        return bookRepo.save(existingBook);
    }
} 