package com.nhnacademy.book_data_batch.infrastructure.repository;

import com.nhnacademy.book_data_batch.domain.Category;
import java.util.Collection;
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

    @Query("SELECT c FROM Category c WHERE c.id IN :ids")
    List<Category> findAllByIdIn(@Param("ids") Collection<Long> ids);
}

