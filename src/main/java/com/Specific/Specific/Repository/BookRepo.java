package com.Specific.Specific.Repository;

import com.Specific.Specific.Models.Entities.Book;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BookRepo extends JpaRepository<Book, Long> {
    List<Book> findByUserId(Long userId);
    List<Book> findByTitleContainingIgnoreCase(String title);
}
