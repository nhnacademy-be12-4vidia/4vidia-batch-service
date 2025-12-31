package com.nhnacademy.book_data_batch.domain.repository;

import com.nhnacademy.book_data_batch.domain.entity.Category;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CategoryRepository extends JpaRepository<Category, Long> {

    Optional<Category> findByKdcCode(String kdcCode);

    boolean existsByKdcCode(String kdcCode);

    @Query("SELECT c FROM Category c WHERE c.path LIKE CONCAT(:pathPrefix, '%')")
    List<Category> findDescendantsByPathPrefix(@Param("pathPrefix") String pathPrefix);

    @Query("SELECT c.id FROM Category c WHERE c.path LIKE CONCAT(:pathPrefix, '%')")
    List<Long> findDescendantIdsByPathPrefix(@Param("pathPrefix") String pathPrefix);

    @Query("SELECT c.path FROM Category c WHERE c.id = :categoryId")
    String findPathByCategoryId(@Param("categoryId") Long categoryId);

    @Query("SELECT c FROM Category c LEFT JOIN FETCH c.parentCategory")
    List<Category> findAllWithParent();
}

