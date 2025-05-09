package com.Specific.Specific.Services;

import com.Specific.Specific.Except.BookNotFoundException;
import com.Specific.Specific.Models.Entities.Book;
import com.Specific.Specific.Models.Entities.User;
import com.Specific.Specific.Repository.BookRepo;
import com.Specific.Specific.utils.SecurityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class BookService {
    private final BookRepo bookRepo;
    private final SecurityUtils securityUtils;
    private final AuthorizationService authorizationService;
    private final UserService userService;
    
    @Autowired
    public BookService(
            BookRepo bookRepo,
            SecurityUtils securityUtils,
            AuthorizationService authorizationService,
            UserService userService) {
        this.bookRepo = bookRepo;
        this.securityUtils = securityUtils;
        this.authorizationService = authorizationService;
        this.userService = userService;
    }
    
    /**
     * Create a new book
     *
     * @param book The book to create
     * @return The created book
     */
    public Book createBook(Book book) {
        User currentUser = securityUtils.getCurrentUser();
        return createBook(book, currentUser);
    }
    
    /**
     * Create a new book for a specific user
     *
     * @param book The book to create
     * @param user The user who will own the book
     * @return The created book
     */
    public Book createBook(Book book, User user) {
        // Set user ID
        book.setUser(user);
        
        return bookRepo.save(book);
    }
    
    /**
     * Create a new book using Firebase UID
     *
     * @param book The book to create
     * @param firebaseUid The Firebase UID of the user
     * @return The created book
     */
    public Book createBookWithFirebaseUid(Book book, String firebaseUid) {
        User user = userService.findUserByFirebaseUid(firebaseUid);
        return createBook(book, user);
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
     * Get a book by ID for a specific user
     *
     * @param id The ID of the book to get
     * @param user The user requesting the book
     * @return The book
     * @throws BookNotFoundException If the book doesn't exist
     */
    public Book getBookById(Long id, User user) throws BookNotFoundException {
        Book book = bookRepo.findById(id)
            .orElseThrow(() -> new BookNotFoundException("Book not found with ID: " + id));
        
        // Verify the user has access to this book
        if (book.getUser().getId() != user.getId()) {
            throw new BookNotFoundException("Book not found with ID: " + id + " for this user");
        }
        
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
     * Delete a book by ID for a specific user
     *
     * @param id The ID of the book to delete
     * @param user The user requesting deletion
     * @throws BookNotFoundException If the book doesn't exist
     */
    public void deleteBook(Long id, User user) throws BookNotFoundException {
        Book book = bookRepo.findById(id)
            .orElseThrow(() -> new BookNotFoundException("Book not found with ID: " + id));
        
        // Verify the user has access to this book
        if (book.getUser().getId() != user.getId()) {
            throw new BookNotFoundException("Book not found with ID: " + id + " for this user");
        }
        
        bookRepo.delete(book);
    }
    
    /**
     * Get all books for the current user
     *
     * @return List of books
     */
    public List<Book> getUserBooks() {
        User currentUser = securityUtils.getCurrentUser();
        return getUserBooks(currentUser);
    }
    
    /**
     * Get all books for a specific user
     *
     * @param user The user whose books to get
     * @return List of books
     */
    public List<Book> getUserBooks(User user) {
        return bookRepo.findByUserId(user.getId());
    }
    
    /**
     * Get all books for a user with the given Firebase UID
     *
     * @param firebaseUid The Firebase UID of the user
     * @return List of books
     */
    public List<Book> getUserBooksByFirebaseUid(String firebaseUid) {
        User user = userService.findUserByFirebaseUid(firebaseUid);
        return getUserBooks(user);
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
    
    /**
     * Update a book for a specific user
     *
     * @param id The ID of the book to update
     * @param bookDetails The updated book details
     * @param user The user requesting the update
     * @return The updated book
     * @throws BookNotFoundException If the book doesn't exist
     */
    public Book updateBook(Long id, Book bookDetails, User user) throws BookNotFoundException {
        Book existingBook = bookRepo.findById(id)
            .orElseThrow(() -> new BookNotFoundException("Book not found with ID: " + id));

        // Verify the user has access to this book
        if (existingBook.getUser().getId() != user.getId()) {
            throw new BookNotFoundException("Book not found with ID: " + id + " for this user");
        }
        
        // Update fields
        existingBook.setTitle(bookDetails.getTitle());
        
        return bookRepo.save(existingBook);
    }
} 