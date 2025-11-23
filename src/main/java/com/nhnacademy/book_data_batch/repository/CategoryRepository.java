package com.nhnacademy.book_data_batch.repository;

import com.nhnacademy.book_data_batch.entity.Category;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CategoryRepository extends JpaRepository<Category, Long> {

    Optional<Category> findByKdcCode(String kdcCode);

    boolean existsByKdcCode(String kdcCode);

    List<Category> findAllByKdcCodeIn(Collection<String> kdcCodes);
}
