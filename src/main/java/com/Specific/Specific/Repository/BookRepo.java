package com.Specific.Specific.Repository;

import com.Specific.Specific.Models.Book;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BookRepo extends JpaRepository<Book, Long> {
    List<Book> findByUser_id(Long userId);
    List<Book> findByTitleContainingIgnoreCase(String title);
}
