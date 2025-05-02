package com.Specific.Specific.Services;

import com.Specific.Specific.Except.BookNotFoundException;
import com.Specific.Specific.Models.Book;
import com.Specific.Specific.Models.User;
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
     * Add a new book for the current user
     *
     * @param book The book to add
     * @return The saved book with generated ID
     */
    public Book addBook(Book book) {
        User currentUser = securityUtils.getCurrentUser();
        book.setUser_id(currentUser.getId());
        return bookRepo.save(book);
    }
    
    /**
     * Delete a book
     *
     * @param book The book to delete
     */
    public void deleteBook(Book book) {
        authorizationService.verifyResourceOwner(book.getUser_id());
        bookRepo.delete(book);
    }
    
    /**
     * Delete a book by its ID
     *
     * @param bookId The ID of the book to delete
     */
    public void deleteBookById(Long bookId) {
        Book book = findBookById(bookId);
        deleteBook(book);
    }
    
    /**
     * Find a book by its ID
     *
     * @param bookId The ID of the book
     * @return The found book
     * @throws BookNotFoundException if book not found
     */
    public Book findBookById(Long bookId) {
        return bookRepo.findById(bookId)
                .orElseThrow(() -> new BookNotFoundException("Book with ID " + bookId + " not found"));
    }
    
    /**
     * Find all books belonging to the current user
     *
     * @return List of books belonging to the current user
     */
    public List<Book> findCurrentUserBooks() {
        User currentUser = securityUtils.getCurrentUser();
        return bookRepo.findByUser_id(currentUser.getId());
    }
    
    /**
     * Search for books by title
     *
     * @param title The title to search for
     * @return List of books matching the title
     */
    public List<Book> searchBooksByTitle(String title) {
        return bookRepo.findByTitleContainingIgnoreCase(title);
    }
    
    /**
     * Update a book
     *
     * @param bookId The ID of the book to update
     * @param newBook The new book data
     * @return The updated book
     */
    public Book updateBook(Long bookId, Book newBook) {
        Book existingBook = findBookById(bookId);
        
        // Verify ownership
        authorizationService.verifyResourceOwner(existingBook.getUser_id());
        
        // Update fields
        existingBook.setTitle(newBook.getTitle());
        existingBook.setDescription(newBook.getDescription());
        
        return bookRepo.save(existingBook);
    }
} 