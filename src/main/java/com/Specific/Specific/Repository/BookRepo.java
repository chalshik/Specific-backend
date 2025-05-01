package com.Specific.Specific.Repository;

import com.Specific.Specific.Models.Book;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BookRepo extends JpaRepository<Book,Long> {
}
